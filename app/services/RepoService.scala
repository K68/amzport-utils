package services

import java.time.{OffsetDateTime, ZoneId}

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.cache.SyncCacheApi
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import repo.RepoCase.{ApiAssetsRow, MonitorRow, ObserverRow, UserRow}
import repo.RepoDao
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RepoService @Inject()(actorSystem: ActorSystem,
                            val dbConfigProvider: DatabaseConfigProvider,
                            val configuration: Configuration,
                            val cache: SyncCacheApi,
                            val repoDao: RepoDao)
                           (implicit val executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import repo.MyPostgresDriver.api._
  import com.roundeights.hasher.Implicits._

  val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

  val saltValue = "mol-rand-salt"

  val ecBlocking: ExecutionContext = actorSystem.dispatchers.lookup("blockingPool")

  def queryAllUser(offset: Int, size: Int, search: Option[String], apiCount: Option[Int], sortBy: String) = {
    val _query = repoDao.User.filter { row =>
      List(
        search.map(i => row.loginName like s"%$i%"),
        apiCount.map(row.apiKeyLimit === _)
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }

    val query = if (sortBy == "lastLoginTime+") {
      _query.sortBy(_.lastLoginTime)
    } else if (sortBy == "apiKeyLimit+") {
      _query.sortBy(_.apiKeyLimit)
    } else if (sortBy == "id+") {
      _query.sortBy(_.id)
    } else if (sortBy == "lastLoginTime-") {
      _query.sortBy(_.lastLoginTime.desc)
    } else if (sortBy == "apiKeyLimit-") {
      _query.sortBy(_.apiKeyLimit.desc)
    } else if (sortBy == "id-") {
      _query.sortBy(_.id.desc)
    } else {
      _query.sortBy(_.id.desc)
    }

    db.run {
      for {
        seq <- query.drop(offset).take(size).result
        count <- query.length.result
      } yield {
        (seq, count)
      }
    }
  }

  def queryUserApiKey(userId: Int): Future[Seq[ApiAssetsRow]] = {
    db.run(repoDao.ApiAssets.filter(_.userId === userId).result)
  }

  def queryUserObserver(offset: Int, size: Int, userId: Option[Int], apiKey: Option[Int], name: Option[String]): Future[(Seq[ObserverRow], Int)] = {
    val query = repoDao.Observer.filter { row =>
      List(
        userId.map(row.userId === _),
        apiKey.map(row.apiAssetsId === _),
        name.map(i => row.nickname like s"%$i%")
      ).collect({ case Some(criteria) => criteria }).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }

    db.run {
      for {
        seq <- query.sortBy(_.id.desc).drop(offset).take(size).result
        count <- query.length.result
      } yield {
        (seq, count)
      }
    }
  }

  def queryObserverMonitor(observerId: Int, startTime: OffsetDateTime, endTime: OffsetDateTime, userId: Option[Int]): Future[Seq[MonitorRow]] = {
    var query = repoDao.Monitor.filter(_.observerId === observerId).filter(_.timeshoot >= startTime).filter(_.timeshoot < endTime)
    if (userId.isDefined) {
      query = query.filter(_.userId === userId)
    }
    db.run(query.sortBy(_.timeshoot).result)
  }

  def queryObserverMonitorByOpenLink(openLink: String, startTime: OffsetDateTime, endTime: OffsetDateTime): Future[Seq[MonitorRow]] = {
    var query = repoDao.Observer.filter(_.openLink === openLink).join(repoDao.Monitor)
      .on(_.id === _.observerId).filter(_._2.timeshoot >= startTime).filter(_._2.timeshoot < endTime).map(_._2)
    db.run(query.sortBy(_.timeshoot).result)
  }

  def checkLogin(name: String, password: String): Future[Option[(Int, Int)]] = {
    val passwd = password.salt(saltValue).sha1.hex
    db.run(repoDao.User.filter(_.loginName === name)
      .filter(_.loginPassword === passwd)
      .map(i => (i.id, i.userType)).result.headOption)
  }

  def addUser(userType: Int, loginName: String, password: String, apiLimit: Int) = {
    // userType: 0, 1, 2   0超级管理员，1子管理员，2普通用户
    val passwd = password.salt(saltValue).sha1.hex
    repoDao.UserAuto.insert(UserRow(0, userType, loginName, passwd, apiLimit, OffsetDateTime.now(zoneId)))
  }

  def updateUserPassword(userId: Int, newPassword: String) = {
    val password = if (newPassword.isEmpty) {
      ""
    } else {
      newPassword.salt(saltValue).sha1.hex
    }
    db.run(repoDao.User.filter(_.id === userId).map(_.loginPassword).update(password))
  }

  def updateUserApiLimit(userId: Int, apiLimit: Int) = {
    db.run(repoDao.User.filter(_.id === userId).map(_.apiKeyLimit).update(apiLimit))
  }

  def updateSelfPassword(userId: Int, oldPassword: String, newPassword: String) = {
    val passwd = oldPassword.salt(saltValue).sha1.hex
    val passwdNew = newPassword.salt(saltValue).sha1.hex
    db.run(repoDao.User.filter(_.id === userId).filter(_.loginPassword === passwd).map(_.loginPassword).update(passwdNew))
  }

  def addSelfApiKey(userId: Int) = {
    repoDao.UserAuto.getByIdOption(userId).flatMap {
      case Some(u) =>
        db.run(repoDao.ApiAssets.filter(_.userId === userId).length.result).flatMap { count =>
          if (count >= u.apiKeyLimit) {
            Future.successful(None)
          } else {
            val newApiKey = s"$userId+++${OffsetDateTime.now(zoneId).toInstant.toEpochMilli}".salt(saltValue).sha1.hex
            repoDao.ApiAssetsAuto.insert(ApiAssetsRow(0, userId, OffsetDateTime.now(zoneId), newApiKey)).map(Some(_))
          }
        }
      case None =>
        Future.successful(None)
    }
  }

  def updateSelfApiKey(userId: Int, keyId: Int) = {
    val newApiKey = s"$userId+++${OffsetDateTime.now(zoneId).toInstant.toEpochMilli}".salt(saltValue).sha1.hex
    db.run(repoDao.ApiAssets.filter(_.userId === userId).filter(_.id === keyId).map(_.keyValue).update(newApiKey))
  }


  // 通过Api调用的接口

  def syncMonitor(apiKey: String, logs: List[String]): Future[Boolean] = {
    db.run(repoDao.ApiAssets.filter(_.keyValue === apiKey).map(i => (i.userId, i.id)).result.headOption).flatMap {
      case Some(ids) =>
        Future.sequence(logs.map{ l =>
          val la = l.split(",")
          // s"$obsvName,$obsvUrl,$costTime,$total,$alive,$timestamp"
          db.run(repoDao.Observer.filter(_.nickname === la(0)).filter(_.observeUrl === la(1)).map(_.id).result.headOption).map {
            case Some(obId) =>
              Some(MonitorRow(0, obId, ids._1, ids._2, OffsetDateTime.parse(la(5)), la(4).toInt, la(3).toInt))
            case None =>
              None
          }
        })(implicitly, ecBlocking).flatMap { ms =>
          val toAdd = ms.filter(_.isDefined).map(_.get)
          db.run(repoDao.Monitor returning repoDao.Monitor.map(_.id) ++= toAdd).map(_ => true)
        }
      case None =>
        Future.successful(false)
    }
  }

  def fetchAllObservers(apiKey: String): Future[(Seq[ObserverRow], Int)] = {
    db.run(repoDao.ApiAssets.filter(_.keyValue === apiKey).map(i => (i.userId, i.id)).result.headOption).flatMap {
      case Some(ids) =>
        queryUserObserver(0, 5000, Some(ids._1), Some(ids._2), None)
      case None =>
        Future.successful((Seq.empty[ObserverRow], 0))
    }
  }

  def addOneObserver(apiKey: String, nickname: String, observeUrl: String): Future[Boolean] = {
    db.run(repoDao.ApiAssets.filter(_.keyValue === apiKey).map(i => (i.userId, i.id)).result.headOption).flatMap {
      case Some(ids) =>
        val openLink = nickname.salt(saltValue).crc32.hex + observeUrl.salt(saltValue).crc32.hex
        repoDao.ObserverAuto.insert(ObserverRow(0, ids._1, ids._2, nickname, observeUrl, OffsetDateTime.now(zoneId), openLink)).map(_ => true)
      case None =>
        Future.successful(false)
    }
  }

  def removeOneObserver(apiKey: String, nickname: String, observeUrl: String): Future[Boolean] = {
    db.run(repoDao.ApiAssets.filter(_.keyValue === apiKey).map(i => (i.userId, i.id)).result.headOption).flatMap {
      case Some(ids) =>
        db.run {
          for {
            observerId <- repoDao.Observer.filter(_.userId === ids._1).filter(_.apiAssetsId === ids._2)
              .filter(_.nickname === nickname).filter(_.observeUrl === observeUrl).map(_.id).result.headOption
            _ <- repoDao.Monitor.filter(_.observerId === observerId).delete
            _ <- repoDao.Observer.filter(_.id === observerId).delete
          } yield {
            true
          }
        }
      case None =>
        Future.successful(false)
    }
  }

  def updateOneObserver(apiKey: String, nickname: String, observeUrl: String, nicknameOld: String, observeUrlOld: String): Future[Boolean] = {
    db.run(repoDao.ApiAssets.filter(_.keyValue === apiKey).map(i => (i.userId, i.id)).result.headOption).flatMap {
      case Some(ids) =>
        val openLink = nickname.salt(saltValue).crc32 + observeUrl.salt(saltValue).crc32
        db.run(repoDao.Observer.filter(_.userId === ids._1).filter(_.apiAssetsId === ids._2)
          .filter(_.nickname === nicknameOld).filter(_.observeUrl === observeUrlOld)
          .map(i => (i.nickname, i.observeUrl, i.openLink)).update((nickname, observeUrl, openLink))).map(_ => true)
      case None =>
        Future.successful(false)
    }
  }

  def addAllObservers(apiKey: String, nicknames: List[String], observeUrls: List[String]): Future[Boolean] = {
    db.run(repoDao.ApiAssets.filter(_.keyValue === apiKey).map(i => (i.userId, i.id)).result.headOption).flatMap {
      case Some(ids) =>
        val now = OffsetDateTime.now(zoneId)
        Future.sequence(nicknames.zip(observeUrls).map { item =>
          db.run(repoDao.Observer.filter(_.userId === ids._1).filter(_.apiAssetsId === ids._2)
            .filter(_.nickname === item._1).filter(_.observeUrl === item._2).exists.result).flatMap {
            case true =>
              Future.successful(false)
            case false =>
              val openLink = item._1.salt(saltValue).crc32 + item._2.salt(saltValue).crc32
              repoDao.ObserverAuto.insert(ObserverRow(0, ids._1, ids._2, item._1, item._2, now, openLink)).map(_ => true)
          }
        })(implicitly, ecBlocking).map(_ => true)
      case None =>
        Future.successful(false)
    }
  }

}
