package controllers

import java.time.OffsetDateTime

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.libs.json.Json.obj
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import services.RepoService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoteController @Inject()(cc: ControllerComponents,
                                 repoService: RepoService,
                                 implicit val executionContext: ExecutionContext
                                ) extends AbstractController(cc) {

  // 权限控制
  def queryAllUser(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("userType") match {
      case Some(userType) =>
        if (userType == "0" || userType == "1") {
          val offset = (req.body \ "offset").as[Int]
          val size = (req.body \ "size").as[Int]
          val search = (req.body \ "search").asOpt[String]
          val apiCount = (req.body \ "apiCount").asOpt[Int]
          val sortBy = (req.body \ "sortBy").as[String]

          repoService.queryAllUser(offset, size, search, apiCount, sortBy).map { res =>
            Ok(obj(fields = "data" -> res,  "status" -> "success"))
          }
        } else {
          Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def queryUserApiKey(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("authed") match {
      case Some(_userId) =>
        val userId = (req.body \ "userId").as[Int]
        if (_userId == userId.toString) {
          repoService.queryUserApiKey(userId).map { res =>
            Ok(obj(fields = "data" -> res, "status" -> "success"))
          }
        } else {
          Future.successful(Ok(obj(fields = "data" -> "用户ID不一致",  "status" -> "error")))
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def queryUserObserver(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("authed") match {
      case Some(_userId) =>
        val offset = (req.body \ "offset").as[Int]
        val size = (req.body \ "size").as[Int]
        val apiKey = (req.body \ "apiKey").asOpt[Int]
        val name = (req.body \ "name").asOpt[String]

        val _userType = req.session.get("userType").get
        if (_userType == "2") {
          val userId = (req.body \ "userId").as[Int]
          if (_userId == userId.toString) {
            repoService.queryUserObserver(offset, size, Some(userId), apiKey, name).map { res =>
              Ok(obj(fields = "data" -> res,  "status" -> "success"))
            }
          } else {
            Future.successful(Ok(obj(fields = "data" -> "用户ID不一致",  "status" -> "error")))
          }
        } else {
          val userId = (req.body \ "userId").asOpt[Int]

          repoService.queryUserObserver(offset, size, userId, apiKey, name).map { res =>
            Ok(obj(fields = "data" -> res,  "status" -> "success"))
          }
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def queryObserverMonitor(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("authed") match {
      case Some(_userId) =>
        val observerId = (req.body \ "observerId").as[Int]
        val startTime = (req.body \ "startTime").as[OffsetDateTime]
        val endTime = (req.body \ "endTime").as[OffsetDateTime]

        val _userType = req.session.get("userType").get
        if (_userType == "2") {
          repoService.queryObserverMonitor(observerId, startTime, endTime, Some(_userId.toInt)).map { res =>
            Ok(obj(fields = "data" -> res,  "status" -> "success"))
          }
        } else {
          repoService.queryObserverMonitor(observerId, startTime, endTime, None).map { res =>
            Ok(obj(fields = "data" -> res,  "status" -> "success"))
          }
        }

      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def login(): Action[JsValue] = Action.async(parse.json) { req =>
    val username = (req.body \ "username").as[String]
    val password = (req.body \ "password").as[String]

    repoService.checkLogin(username, password).map {
      case Some(meta) =>
        Ok(obj(fields = "data" -> s"${meta._1},${meta._2}",  "status" -> "success"))
          .withSession(req.session + ("authed" -> meta._1.toString) + ("userType" -> meta._2.toString))
      case None =>
        Ok(obj(fields = "data" -> "错误可能：1.密码错误 2.初始密码为空",  "status" -> "error"))
    }
  }

  def logout(): Action[JsValue] = Action.async(parse.json) { req =>
    val timestamp = (req.body \ "timestamp").as[OffsetDateTime]
    val timeAbs = Math.abs(OffsetDateTime.now().toEpochSecond - timestamp.toEpochSecond)
    if (timeAbs < 3600) {
      Future.successful(Ok(obj(fields = "data" -> true,  "status" -> "success")).withNewSession)
    } else {
      Future.successful(Ok(obj(fields = "data" -> "错误可能：请求不合法",  "status" -> "error")))
    }
  }

  def addUser(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("userType") match {
      case Some(_userType) =>
        val userType = (req.body \ "userType").as[Int]
        val loginName = (req.body \ "loginName").as[String]
        val password = (req.body \ "password").as[String]
        val apiLimit = (req.body \ "apiLimit").as[Int]

        if (_userType == "0") {
          repoService.addUser(userType, loginName, password, apiLimit).map { res =>
            Ok(obj(fields = "data" -> res,  "status" -> "success"))
          }
        } else if(_userType == "1") {
          repoService.addUser(2, loginName, password, apiLimit).map { res =>
            Ok(obj(fields = "data" -> res,  "status" -> "success"))
          }
        } else {
          Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def updateUserPassword(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("userType") match {
      case Some(_userType) =>
        if (_userType == "0") {
          val userId = (req.body \ "userId").as[Int]
          val newPassword = (req.body \ "newPassword").as[String]

          repoService.updateUserPassword(userId, newPassword).map { res =>
            Ok(obj(fields = "data" -> res,  "status" -> "success"))
          }
        } else {
          Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def updateUserApiLimit(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("userType") match {
      case Some(_userType) =>
        if (_userType == "0" || _userType == "1") {
          val userId = (req.body \ "userId").as[Int]
          val apiLimit = (req.body \ "apiLimit").as[Int]

          repoService.updateUserApiLimit(userId, apiLimit).map { res =>
            Ok(obj(fields = "data" -> res,  "status" -> "success"))
          }
        } else {
          Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def updateSelfPassword(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("authed") match {
      case Some(_userId) =>
        val userId = (req.body \ "userId").as[Int]
        val oldPassword = (req.body \ "oldPassword").as[String]
        val newPassword = (req.body \ "newPassword").as[String]

        if (_userId == userId.toString) {
          repoService.updateSelfPassword(userId, oldPassword, newPassword).map { res =>
            Ok(obj(fields = "data" -> res, "status" -> "success"))
          }
        } else {
          Future.successful(Ok(obj(fields = "data" -> "用户ID不一致",  "status" -> "error")))
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def addSelfApiKey(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("authed") match {
      case Some(_userId) =>
        val userId = (req.body \ "userId").as[Int]

        if (_userId == userId.toString) {
          repoService.addSelfApiKey(userId).map {
            case Some(_) =>
              Ok(obj(fields = "data" -> true,  "status" -> "success"))
            case None =>
              Ok(obj(fields = "data" -> false,  "status" -> "error"))
          }
        } else {
          Future.successful(Ok(obj(fields = "data" -> "用户ID不一致",  "status" -> "error")))
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def updateSelfApiKey(): Action[JsValue] = Action.async(parse.json) { req =>
    req.session.get("authed") match {
      case Some(_userId) =>
        val userId = (req.body \ "userId").as[Int]
        val keyId = (req.body \ "keyId").as[Int]

        if (_userId == userId.toString) {
          repoService.updateSelfApiKey(userId, keyId).map { res =>
            Ok(obj(fields = "data" -> res,  "status" -> "success"))
          }
        } else {
          Future.successful(Ok(obj(fields = "data" -> "用户ID不一致",  "status" -> "error")))
        }
      case None =>
        Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    }
  }

  def customObserver(): Action[JsValue] = Action.async(parse.json) { req =>
    val openLink = (req.body \ "openLink").as[String]
    val startTime = (req.body \ "startTime").as[OffsetDateTime]
    val endTime = (req.body \ "endTime").as[OffsetDateTime]

    if (startTime.plusDays(60).isBefore(endTime)) {
      Future.successful(Ok(obj(fields = "data" -> "请求不合法",  "status" -> "error")))
    } else {
      repoService.queryObserverMonitorByOpenLink(openLink, startTime, endTime).map { res =>
        Ok(obj(fields = "data" -> res,  "status" -> "success"))
      }
    }
  }

  // 通过Api调用的接口

  def fetchAllObservers(): Action[JsValue] = Action.async(parse.json) { req =>
    val apiKey = (req.body \ "apiKey").as[String]

    repoService.fetchAllObservers(apiKey).map { res =>
      Ok(obj(fields = "data" -> res,  "status" -> "success"))
    }
  }

  def addOneObserver(): Action[JsValue] = Action.async(parse.json) { req =>
    val apiKey = (req.body \ "apiKey").as[String]
    val nickname = (req.body \ "nickname").as[String]
    val observeUrl = (req.body \ "observeUrl").as[String]

    repoService.addOneObserver(apiKey, nickname, observeUrl).map {
      case true =>
        Ok(obj(fields = "data" -> true,  "status" -> "success"))
      case false =>
        Ok(obj(fields = "data" -> false,  "status" -> "error"))
    }
  }

  def removeOneObserver(): Action[JsValue] = Action.async(parse.json) { req =>
    val apiKey = (req.body \ "apiKey").as[String]
    val nickname = (req.body \ "nickname").as[String]
    val observeUrl = (req.body \ "observeUrl").as[String]

    repoService.removeOneObserver(apiKey, nickname, observeUrl).map {
      case true =>
        Ok(obj(fields = "data" -> true,  "status" -> "success"))
      case false =>
        Ok(obj(fields = "data" -> false,  "status" -> "error"))
    }
  }

  def updateOneObserver(): Action[JsValue] = Action.async(parse.json) { req =>
    val apiKey = (req.body \ "apiKey").as[String]
    val nickname = (req.body \ "nickname").as[String]
    val observeUrl = (req.body \ "observeUrl").as[String]
    val nicknameOld = (req.body \ "nicknameOld").as[String]
    val observeUrlOld = (req.body \ "observeUrlOld").as[String]

    repoService.updateOneObserver(apiKey, nickname, observeUrl, nicknameOld, observeUrlOld).map {
      case true =>
        Ok(obj(fields = "data" -> true,  "status" -> "success"))
      case false =>
        Ok(obj(fields = "data" -> false,  "status" -> "error"))
    }
  }

  def addAllObservers(): Action[JsValue] = Action.async(parse.json) { req =>
    val apiKey = (req.body \ "apiKey").as[String]
    val nicknames = (req.body \ "nicknames").as[List[String]]
    val observeUrls = (req.body \ "observeUrls").as[List[String]]

    repoService.addAllObservers(apiKey, nicknames, observeUrls).map {
      case true =>
        Ok(obj(fields = "data" -> true,  "status" -> "success"))
      case false =>
        Ok(obj(fields = "data" -> false,  "status" -> "error"))
    }
  }

  def syncFetchMonitorToRemote(): Action[JsValue] = Action.async(parse.json) { req =>
    val apiKey = (req.body \ "apiKey").as[String]
    val logs = (req.body \ "logs").as[List[String]]

    repoService.syncMonitor(apiKey, logs).map {
      case true =>
        Ok(obj(fields = "data" -> true,  "status" -> "success"))
      case false =>
        Ok(obj(fields = "data" -> false,  "status" -> "error"))
    }
  }

}
