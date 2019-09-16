package services

import java.io.File

import com.google.common.io.Files
import com.softwaremill.sttp.HttpURLConnectionBackend
import javax.inject.{Inject, Singleton}
import play.api.Environment
import play.api.cache.AsyncCacheApi
import play.api.libs.json.Json
import com.softwaremill.sttp._
import com.roundeights.hasher.Implicits._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiService @Inject() (
                             environment: Environment,
                             cache: AsyncCacheApi,
                             implicit val executionContext: ExecutionContext
                           ) {

  implicit val backend = HttpURLConnectionBackend()
  val saltValue = "AMZPORT_UTIL"

  def checkLocalLogin(localPasswd: String): Future[Boolean] = {
    getApiKey().map { keys =>
      if (keys._1.isEmpty) {
        false
      } else {
        val passwd = localPasswd.salt(saltValue).sha1.hex
        if (keys._1 == passwd) {
          true
        } else {
          false
        }
      }
    }
  }

  def setApiKey(apiKey: String, localPasswd: String): Future[String] = {
    getApiKey().flatMap { keys =>
      if (keys._2.isEmpty) {
        _setApiKey(apiKey, localPasswd)
      } else if (keys._2 == apiKey && localPasswd.nonEmpty) {
        _setApiKey(apiKey, localPasswd)
      } else {
        println("设置API_KEY和本地密码不成功")
        Future.successful("")
      }
    }
  }

  private def _setApiKey(apiKey: String, localPasswd: String): Future[String] = {
    val passwd = localPasswd.salt(saltValue).sha1.hex
    val setToCache = s"$passwd#!#$apiKey"
    cache.set("API_KEY", apiKey, Duration.Inf).map { _ =>
      val rootPath = environment.rootPath
      val outPut = new File(rootPath, "meta.json")
      val meta = Json.obj("api" -> apiKey)
      try {
        Files.write(meta.toString().getBytes, outPut)
        ""
      } catch {
        case e: Exception =>
          e.getMessage
      }
    }
  }

  def getApiKey(): Future[(String, String)] = {
    cache.getOrElseUpdate("API_KEY", Duration.Inf) {
      val rootPath = environment.rootPath
      val inPut = new File(rootPath, "meta.json")
      if (inPut.exists() && inPut.isFile) {
        try {
          val inJson = Json.parse(Files.toByteArray(inPut))
          val apiKey = (inJson \ "api").as[String]
          Future.successful(apiKey)
        } catch {
          case e: Exception =>
            println(e.getMessage)
            Future.successful("")
        }
      } else {
        Future.successful("")
      }
    }.map { res =>
      val keys = res.split("#!#")
      if (keys.length < 2) {
        ("", "")
      } else {
        (keys(0), keys(1))
      }
    }
  }

  def updateAllObservers(observers: Array[(String, String)]): Future[Boolean] = {
    // TODO
    Future.successful(true)
  }

  def addOneObserver(observer: (String, String)): Future[Boolean] = {
    Future.successful(true)
  }

  def removeOneObserver(observer: (String, String)): Future[Boolean] = {
    Future.successful(true)
  }

  def updateOneObserver(observerOld: (String, String), observerNew: (String, String)): Future[Boolean] = {
    Future.successful(true)
  }

  def fetchAllObservers(): Future[Array[(String, String)]] = {
    // TODO
    Future.successful(Array[(String, String)]())
  }

}
