package services

import java.io.File
import java.nio.charset.Charset
import java.time.ZoneId

import akka.actor.ActorSystem
import akka.util.Timeout
import com.google.common.io.Files
import javax.inject._
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import com.softwaremill.sttp._
import services.ObserverManager.SmsEntity

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class MainService @Inject() (appLifecycle: ApplicationLifecycle,
                             actorSystem: ActorSystem,
                             configuration: Configuration,
                             implicit val executionContext: ExecutionContext
                            ) {
  private implicit val timeout: Timeout = 3600.seconds
  private implicit val backend = HttpURLConnectionBackend()

  private val WHITE_LIST_PATH = configuration.underlying.getString("WHITE_LIST_PATH")
  private val SMS_POST_URL = configuration.underlying.getString("SMS_POST_URL")
  private val SMS_AUTH_ACCOUNT = configuration.underlying.getString("SMS_AUTH_ACCOUNT")
  private val SMS_AUTH_PSWD = configuration.underlying.getString("SMS_AUTH_PSWD")
  private val SMS_REMIAN_LIMIT = configuration.underlying.getInt("SMS_REMIAN_LIMIT")
  private val SMS_SUB_URL_SEND = "/mt.ashx"
  private val SMS_SUB_URL_STAT = "/bi.ashx"

  private val SMS_GLOBAL_URL = configuration.underlying.getString("SMS_GLOBAL_URL")
  private val SMS_GLOBAL_FROM = configuration.underlying.getString("SMS_GLOBAL_FROM")
  private val SMS_GLOBAL_U = configuration.underlying.getString("SMS_GLOBAL_U")
  private val SMS_GLOBAL_P = configuration.underlying.getString("SMS_GLOBAL_P")

  private val SMS_I18N_EN = configuration.underlying.getBoolean("SMS_I18N_EN")

  private val zoneId = ZoneId.of("Asia/Shanghai")
  private val ecBlocking = actorSystem.dispatchers.lookup("BlockingPool")

  private var whiteListSet = Set.empty[String]

  actorSystem.scheduler.scheduleWithFixedDelay(10.seconds, 2.minutes) { () =>
    importWhiteListFile(WHITE_LIST_PATH)
  }

  actorSystem.scheduler.scheduleWithFixedDelay(2.minutes, 24.hours) { () =>
    smsBi() match {
      case Some(stat) =>
        val stats = stat.split(',')
        if (stats.length > 2 && stats(2).trim.nonEmpty) {
          val remain = Try(stats(2).trim.toInt)
          if (remain.isSuccess && remain.get <= SMS_REMIAN_LIMIT) {
            smsDynamicCode("18667436829", remain.get.toString).map(i => println(i))
          }
        }
      case None =>
    }
  }

  ObserverManager.subscribe((entity: SmsEntity) => {
    smsMt(entity.pn, entity.msg, realSend = true)
    true
  })

  private def importWhiteListFile(path: String): Unit = {
    val importFile = new File(path)
    if (importFile.exists() && importFile.canRead) {
      val rows = Files.asCharSource(importFile, Charset.forName("utf-8")).readLines()
      if (rows.size() > 0) {
        var tmpSet = Set.empty[String]
        rows.forEach { row =>
          if (row.nonEmpty && !tmpSet.contains(row)) {
            tmpSet = tmpSet.+(row)
          }
        }

        whiteListSet = tmpSet.intersect(whiteListSet).union(tmpSet)
      }
    }
  }

  def addressInWhiteList(address: String): Boolean = {
    whiteListSet.contains(address)
  }

  def smsVerificationCode(pn: String, code: String): Future[Option[String]] = {
    val msg = s"【验证码】您的验证码是：$code"
    Future(smsMt(pn, msg))(ecBlocking)
  }

  def smsDynamicCode(pn: String, code: String): Future[Option[String]] = {
    val msg = s"【动态口令】您的动态口令是：$code"
    Future(smsMt(pn, msg))(ecBlocking)
  }

  def smsCodeDIY(pn: String, code: String, tpl: String): Future[Option[String]] = {
    val msg = if (SMS_I18N_EN) {
      s"【PIN】$code is your $tpl confirmation code"
    } else {
      s"【$tpl】您的验证码是：$code"
    }
    Future(smsMt(pn, msg))(ecBlocking)
  }

  private def smsMt(pn: String, msg: String, realSend: Boolean = false): Option[String] = {

    if (!realSend) {
      ObserverManager.notify(SmsEntity(pn, msg))
    } else {
      if (pn.startsWith("+") && !pn.startsWith("+86")) {
        smsGlobal(pn, msg)

      } else {
        val _pn = if (pn.startsWith("+86")) {
          pn.substring(3)
        } else {
          pn
        }
        val url = s"$SMS_POST_URL$SMS_SUB_URL_SEND"
        val rep = sttp.post(uri"$url").body(Map[String, String](
          "pn" -> _pn,
          "account" -> SMS_AUTH_ACCOUNT,
          "pswd" -> SMS_AUTH_PSWD,
          "msg" -> msg
        )).send()

        rep.body match {
          case Right(x) => Some(x)
          case Left(e) =>
            println(s"SMS MT Exception: $e")
            None
        }
      }

    }
  }

  def smsBi(): Option[String] = {
    val url = s"$SMS_POST_URL$SMS_SUB_URL_STAT"
    val rep = sttp.post(uri"$url").body(Map[String, String](
      "account" -> SMS_AUTH_ACCOUNT,
      "pswd" -> SMS_AUTH_PSWD
    )).send()

    rep.body match {
      case Right(x) => Some(x)
      case Left(e) =>
        println(s"SMS BI Exception: $e")
        None
    }
  }

  private def smsGlobal(pn: String, msg: String): Option[String] = {
    val rep = sttp.auth.basic(SMS_GLOBAL_U, SMS_GLOBAL_P)
      .post(uri"$SMS_GLOBAL_URL")
      .body(Map[String, String](
        "From" -> SMS_GLOBAL_FROM,
        "To" -> pn,
        "Body" -> msg
      )).send()

    rep.body match {
      case Right(x) => Some(x)
      case Left(e) =>
        println(s"SMS Global Exception: $e")
        None
    }
  }

  def smsBatchDIY(accountList: List[String], tpl: String): Future[List[String]] = {
    Future(smsMts(accountList, tpl))(ecBlocking)
  }

  private def smsMts(accountList: List[String], tpl: String): List[String] = {
    accountList.map{ list =>
      val strs = list.split(";", 2)
      val pn = strs(0)
      val str = strs(1)
      val msg = s"【$tpl】$str"
      smsMt(pn, msg)
    }.filter(_.isDefined).map(j => j.get)
  }
}
