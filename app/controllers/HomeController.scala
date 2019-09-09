package controllers

import java.time.OffsetDateTime

import com.softwaremill.sttp._
import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._
import services.FetchCenter

import scala.concurrent.Future

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents,
                               fetchCenter: FetchCenter,
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

  implicit val backend = HttpURLConnectionBackend()

  def testFetch = Action.async {
    val startTime = OffsetDateTime.now().toInstant.toEpochMilli
    val req1 = sttp.get(uri"http://120.27.0.232/access/dashboard/?access_key=APIsXTOG8fIDqru5ehlmCpE")
    val rep1 = req1.send()

    val timestamp = OffsetDateTime.now().toInstant.toEpochMilli
    val req2 = sttp.get(uri"http://120.27.0.232/access/user/stats/?_=$timestamp").cookies(rep1)
    val rep2 = req2.send()

    val costTime = OffsetDateTime.now().toInstant.toEpochMilli - startTime

    fetchCenter.testFetch()

    rep2.body match {
      case Right(x) => Future.successful(Ok(Json.obj("status" ->"OK", "costTime" -> costTime, "message" -> x)))
      case Left(x) => Future.successful(Ok(Json.obj("status" ->"OK", "costTime" -> costTime, "message" -> "error")))
    }

//    Future.successful(Ok(Json.obj("status" ->"OK", "message" -> "Hello World")))
  }

}
