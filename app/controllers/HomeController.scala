package controllers

import java.io.File

import com.google.common.io.Files
import javax.inject._
import play.api.Environment
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{JsValue, Json}
import play.api.libs.json.Json.obj
import play.api.mvc._
import services.FetchCenter

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents,
                               environment: Environment,
                               cache: AsyncCacheApi,
                               fetchCenter: FetchCenter,
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

  def setApiKey(): Action[JsValue] = Action.async(parse.json) { req =>
    val apiKey = (req.body \ "apiKey").as[String]
    cache.set("API_KEY", apiKey, Duration.Inf).map { _ =>
      val rootPath = environment.rootPath
      val outPut = new File(rootPath, "meta.json")
      val meta = Json.obj("api" -> apiKey)
      try {
        Files.write(meta.toString().getBytes, outPut)
        Ok(obj(fields = "data" -> true,  "status" -> "success"))
      } catch {
        case e: Exception =>
          Ok(obj(fields = "data" -> e.getMessage,  "status" -> "error"))
      }
    }
  }

  def testFetch(): Action[AnyContent] = Action.async {
    fetchCenter.testFetch().map { result =>
      Ok(Json.obj("status" ->"OK", "data" -> Json.toJson(result)))
    }
//    Future.successful(Ok(Json.obj("status" ->"OK", "message" -> "Hello World")))
  }

}
