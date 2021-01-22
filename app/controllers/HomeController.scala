package controllers

import javax.inject._
import play.api.libs.json._
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

  def smsCodeDIY(): Action[JsValue] = Action.async(parse.json) { req =>
    if (mainService.addressInWhiteList(req.remoteAddress)) {
      val pn = (req.body \ "pn").as[String]
      val code = (req.body \ "code").as[String]
      val tpl = (req.body \ "tpl").as[String]

      mainService.smsCodeDIY(pn, code, tpl).map {
        case Some(result) =>
          Results.Ok(result)
        case None =>
          Results.InternalServerError("短信发送异常")
      }
    } else {
      Future.successful(Results.Forbidden("来源地址不合法"))
    }
  }

  def fetchBi(): Action[AnyContent] = Action.async { req =>
    if (mainService.addressInWhiteList(req.remoteAddress)) {
      println("合理请求来源:" + req.remoteAddress)
      Future(mainService.smsBi()).map {
        case Some(r) =>
          Results.Ok(r)
        case None =>
          Results.Ok("获取短信账户信息出错")
      }
    } else {
      Future.successful(Results.Forbidden("来源地址不合法" + req.remoteAddress))
    }
  }

//  implicit val listStringFormat: OFormat[List[String]] = Json.format[List[String]]

  def smsBatchDIY(): Action[JsValue] = Action.async(parse.json) { req =>
    if (mainService.addressInWhiteList(req.remoteAddress)) {
      val accountLists = (req.body \ "accountList").as[Array[String]].toList
      val tpl = (req.body \ "tpl").as[String]
      mainService.smsBatchDIY(accountLists, tpl).map { str =>
          Results.Ok(str.length.toString)
      }
    } else {
      Future.successful(Results.Forbidden("来源地址不合法"))
    }
  }

}
