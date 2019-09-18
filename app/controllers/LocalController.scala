package controllers

import java.io.File
import java.time.OffsetDateTime

import javax.inject._
import play.api.libs.Files
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json.obj
import play.api.mvc._
import services.{ApiService, FetchCenter}
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import kantan.csv.java8._

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class LocalController @Inject()(cc: ControllerComponents,
                                fetchCenter: FetchCenter,
                                apiService: ApiService,
                                implicit val executionContext: ExecutionContext
                              ) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def testFetch(): Action[AnyContent] = Action.async {
    fetchCenter.testFetch().map { result =>
      Ok(Json.obj("data" -> Json.toJson(result), "status" ->"success"))
    }
    //    Future.successful(Ok(Json.obj("status" ->"OK", "message" -> "Hello World")))
  }

  def setApiKey(): Action[JsValue] = Action.async(parse.json) { req =>
    val apiKey = (req.body \ "apiKey").as[String]
    val localPasswd = (req.body \ "localPasswd").as[String]

    apiService.setApiKey(apiKey, localPasswd).map { res =>
      if (res.isEmpty) {
        Ok(obj(fields = "data" -> true,  "status" -> "success"))
      } else {
        Ok(obj(fields = "data" -> res,  "status" -> "error"))
      }
    }
  }

  def localLogin(): Action[JsValue] = Action.async(parse.json) { req =>
    val localPasswd = (req.body \ "localPasswd").as[String]

    apiService.checkLocalLogin(localPasswd).map {
      case true =>
        Ok(obj(fields = "data" -> true,  "status" -> "success")).withSession(req.session + ("localAuthed" -> "TRUE"))
      case false =>
        Ok(obj(fields = "data" -> "错误可能：1.密码错误 2.初始密码为空",  "status" -> "error"))
    }
  }

  def localLogout(): Action[JsValue] = Action.async(parse.json) { req =>
    val timestamp = (req.body \ "timestamp").as[OffsetDateTime]
    val timeAbs = Math.abs(OffsetDateTime.now().toEpochSecond - timestamp.toEpochSecond)
    if (timeAbs < 5) {
      Future.successful(Ok(obj(fields = "data" -> true,  "status" -> "success")).withNewSession)
    } else {
      Future.successful(Ok(obj(fields = "data" -> "错误可能：请求不合法",  "status" -> "error")))
    }
  }

  def importObservers(): Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { req =>
    req.session.get("localAuthed") match {
      case Some(_) =>
        req.body.file("importData").map { importData =>
          val filename = importData.filename
          val contentType = importData.contentType

          if (importData.ref.exists() && importData.ref.isFile && importData.ref.length() > 0) {
            val importFile = importData.ref.getAbsoluteFile

            if (importFile.exists() && importFile.canRead) {
              val ifReader = importFile.asCsvReader[(String, String)](rfc)
              if (ifReader.size > 3000) {
                Future.successful(Ok(obj(fields = "data" -> "观察者链接超过3000的上限",  "status" -> "error")))
              } else {
                val observers = ifReader.toArray.filter(i => i.isRight).map(i => i.right.get)
                fetchCenter.addAllObservers(observers).map {
                  case true =>
                    Ok(obj(fields = "data" -> true,  "status" -> "success")).withNewSession
                  case false =>
                    Ok(obj(fields = "data" -> "远端同步出错",  "status" -> "error"))
                }
              }
            } else {
              Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
            }
          } else {
            Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
          }
        }.getOrElse(Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error"))))
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def addOneObserver(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("localAuthed") match {
      case Some(_) =>
        val name = (req.body \ "name").as[String]
        val url = (req.body \ "url").as[String]
        fetchCenter.addOneObserver((name, url)).map {
          case true =>
            Ok(obj(fields = "data" -> true,  "status" -> "success"))
          case false =>
            Ok(obj(fields = "data" -> "错误可能：请求不合法",  "status" -> "error"))
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def removeOneObserver(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("localAuthed") match {
      case Some(_) =>
        val name = (req.body \ "name").as[String]
        val url = (req.body \ "url").as[String]
        fetchCenter.removeOneObserver((name, url)).map {
          case true =>
            Ok(obj(fields = "data" -> true,  "status" -> "success"))
          case false =>
            Ok(obj(fields = "data" -> "错误可能：请求不合法",  "status" -> "error"))
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def updateOneObserver(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("localAuthed") match {
      case Some(_) =>
        val name = (req.body \ "name").as[String]
        val url = (req.body \ "url").as[String]
        val newName = (req.body \ "newName").as[String]
        val newUrl = (req.body \ "newUrl").as[String]
        fetchCenter.updateOneObserver((name, url), (newName, newUrl)).map {
          case true =>
            Ok(obj(fields = "data" -> true,  "status" -> "success"))
          case false =>
            Ok(obj(fields = "data" -> "错误可能：请求不合法",  "status" -> "error"))
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

}
