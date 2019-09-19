package services

import java.io.File

import com.google.common.io.Files
import com.softwaremill.sttp.HttpURLConnectionBackend
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import play.api.cache.AsyncCacheApi
import play.api.libs.json.Json
import com.softwaremill.sttp._
import com.roundeights.hasher.Implicits._
import repo.RepoCase.ObserverRow

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApiService @Inject() (
                             environment: Environment,
                             configuration: Configuration,
                             cache: AsyncCacheApi,
                             implicit val executionContext: ExecutionContext
                           ) {

  implicit val backend = HttpURLConnectionBackend()
  val saltValue = "mol-rand-salt"

  val remoteCenterURL = configuration.underlying.getString("RemoteCenterURL")

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

  def addAllObservers(observers: Array[(String, String)]): Future[Boolean] = {
    getApiKey().map { keys =>
      if (keys._2.isEmpty) {
        false
      } else {
        val (nicknames, observeUrls) = observers.unzip
        val toSend = Json.obj("apiKey" -> keys._2, "nicknames" -> nicknames, "observeUrls" -> observeUrls)
        val req = sttp.post(uri"$remoteCenterURL/addAllObservers")
          .header("Content-Type", "application/json")
          .body(Json.stringify(toSend))
        val rep = req.send()

        rep.body match {
          case Right(x) =>
            (Json.parse(x) \ "status").asOpt[String].getOrElse("") == "success"
          case Left(e) =>
            println(s"AddAllObservers Exception: $e")
            false
        }
      }
    }
  }

  def addOneObserver(observer: (String, String)): Future[Boolean] = {
    getApiKey().map { keys =>
      if (keys._2.isEmpty) {
        false
      } else {
        val toSend = Json.obj("apiKey" -> keys._2, "nickname" -> observer._1, "observeUrl" -> observer._2)
        val req = sttp.post(uri"$remoteCenterURL/addOneObserver")
          .header("Content-Type", "application/json")
          .body(Json.stringify(toSend))
        val rep = req.send()

        rep.body match {
          case Right(x) =>
            (Json.parse(x) \ "status").asOpt[String].getOrElse("") == "success"
          case Left(e) =>
            println(s"AddOneObserver Exception: $e")
            false
        }
      }
    }
  }

  def removeOneObserver(observer: (String, String)): Future[Boolean] = {
    getApiKey().map { keys =>
      if (keys._2.isEmpty) {
        false
      } else {
        val toSend = Json.obj("apiKey" -> keys._2, "nickname" -> observer._1, "observeUrl" -> observer._2)
        val req = sttp.post(uri"$remoteCenterURL/removeOneObserver")
          .header("Content-Type", "application/json")
          .body(Json.stringify(toSend))
        val rep = req.send()

        rep.body match {
          case Right(x) =>
            (Json.parse(x) \ "status").asOpt[String].getOrElse("") == "success"
          case Left(e) =>
            println(s"RemoveOneObserver Exception: $e")
            false
        }
      }
    }
  }

  def updateOneObserver(observerOld: (String, String), observerNew: (String, String)): Future[Boolean] = {
    getApiKey().map { keys =>
      if (keys._2.isEmpty) {
        false
      } else {
        val toSend = Json.obj("apiKey" -> keys._2,
          "nickname" -> observerNew._1, "observeUrl" -> observerNew._2,
          "nicknameOld" -> observerOld._1, "observeUrlOld" -> observerOld._2
        )
        val req = sttp.post(uri"$remoteCenterURL/updateOneObserver")
          .header("Content-Type", "application/json")
          .body(Json.stringify(toSend))
        val rep = req.send()

        rep.body match {
          case Right(x) =>
            (Json.parse(x) \ "status").asOpt[String].getOrElse("") == "success"
          case Left(e) =>
            println(s"UpdateOneObserver Exception: $e")
            false
        }
      }
    }
  }

  def fetchAllObservers(): Future[Array[(String, String)]] = {
    getApiKey().map { keys =>
      if (keys._2.isEmpty) {
        Array.empty[(String, String)]
      } else {
        val toSend = Json.obj("apiKey" -> keys._2)
        val req = sttp.post(uri"$remoteCenterURL/fetchAllObservers")
          .header("Content-Type", "application/json")
          .body(Json.stringify(toSend))
        val rep = req.send()

        rep.body match {
          case Right(x) =>
            val result = (Json.parse(x) \ "data").asOpt[(Seq[ObserverRow], Int)]
            if (result.isDefined) {
              result.get._1.map(i => (i.nickname, i.observeUrl)).toArray
            } else {
              Array.empty[(String, String)]
            }
          case Left(e) =>
            println(s"FetchAllObservers Exception: $e")
            Array.empty[(String, String)]
        }
      }
    }
  }

  def syncFetchMonitorToRemote(logs: Seq[String]): Future[Boolean] = {
    getApiKey().map { keys =>
      if (keys._2.isEmpty) {
        false
      } else {
        val toSend = Json.obj("apiKey" -> keys._2, "logs" -> logs)
        val req = sttp.post(uri"$remoteCenterURL/syncFetchMonitorToRemote")
          .header("Content-Type", "application/json")
          .body(Json.stringify(toSend))
        val rep = req.send()

        rep.body match {
          case Right(x) =>
            (Json.parse(x) \ "status").asOpt[String].getOrElse("") == "success"
          case Left(e) =>
            println(s"SyncFetchMonitorToRemote Exception: $e")
            false
        }
      }
    }
  }

}
