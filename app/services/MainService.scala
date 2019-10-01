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
  private val zoneId = ZoneId.of("Asia/Shanghai")
  private val ecBlocking = actorSystem.dispatchers.lookup("BlockingPool")

  private var whiteList = List.empty[String]

  actorSystem.scheduler.schedule(1.minutes, 10.minutes) {
    importWhiteListFile(WHITE_LIST_PATH)
  }

  actorSystem.scheduler.schedule(2.minutes, 24.hours) {
    smsBi() match {
      case Some(stat) =>
        val stats = stat.split(',')
        if (stats.length > 2 && stats(2).nonEmpty) {
          val remain = Try(stats(2).toInt)
          if (remain.isSuccess && remain.get <= SMS_REMIAN_LIMIT) {
            smsDynamicCode("18667436829", remain.get.toString)
          }
        }
      case None =>
    }
  }

  private def importWhiteListFile(path: String): Unit = {
    val importFile = new File(path)
    if (importFile.exists() && importFile.canRead) {
      val rows = Files.asCharSource(importFile, Charset.forName("utf-8")).readLines()
      if (rows.size() > 0) {
        whiteList = List.empty[String]
        rows.forEach { row =>
          if (row.nonEmpty) {
            whiteList = whiteList :+ row
          }
        }
      }
    }
  }

  def addressInWhiteList(address: String): Boolean = {
    whiteList.contains(address)
  }

  def smsVerificationCode(pn: String, code: String): Future[Option[String]] = {
    val msg = s"【验证码】您的验证码是：$code"
    Future(smsMt(pn, msg))(ecBlocking)
  }

  def smsDynamicCode(pn: String, code: String): Future[Option[String]] = {
    val msg = s"【动态口令】您的动态口令是：$code"
    Future(smsMt(pn, msg))(ecBlocking)
  }

  private def smsMt(pn: String, msg: String): Option[String] = {
    val url = s"$SMS_POST_URL$SMS_SUB_URL_SEND"
    val rep = sttp.post(uri"$url").body(Map[String, String](
      "pn" -> pn,
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

  private def smsBi(): Option[String] = {
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

}
