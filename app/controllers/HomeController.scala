package controllers

import javax.inject._
import play.api.libs.json.JsValue
import play.api.mvc._
import services.MainService

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents,
                               mainService: MainService,
                               implicit val executionContext: ExecutionContext) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def verificationCode(): Action[JsValue] = Action.async(parse.json) { req =>
    if (mainService.addressInWhiteList(req.remoteAddress)) {
      val pn = (req.body \ "pn").as[String]
      val code = (req.body \ "code").as[String]

      mainService.smsVerificationCode(pn, code).map {
        case Some(result) =>
          Results.Ok(result)
        case None =>
          Results.InternalServerError("短信发送异常")
      }
    } else {
      Future.successful(Results.Forbidden("来源地址不合法"))
    }
  }

}
