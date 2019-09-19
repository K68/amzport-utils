package services

import java.time.{OffsetDateTime, ZoneId}

import actors.{Fetcher, Record}
import akka.actor.ActorSystem
import javax.inject._
import play.api.inject.ApplicationLifecycle
import akka.pattern.ask

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import actors.Fetcher._
import akka.util.Timeout
import play.api.Configuration

@Singleton
class FetchCenter @Inject() (appLifecycle: ApplicationLifecycle,
                             actorSystem: ActorSystem,
                             configuration: Configuration,
                             val fetchService: FetchService,
                             val apiService: ApiService,
                             implicit val executionContext: ExecutionContext
                            ) {
  val fetchers = List.range(1, 6).map(i => actorSystem.actorOf(Fetcher.props(this), s"fetcher_$i"))
  val recorder = actorSystem.actorOf(Record.props(), "recorder")

  val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")
  implicit val timeout: Timeout = 3600.seconds

  val ecBlocking: ExecutionContext = actorSystem.dispatchers.lookup("blockingPool")

  val fetchMode = configuration.underlying.getString("FetchMode")

  var observersCache = List[(String, String)]()

  actorSystem.scheduler.schedule(5.minutes, 60.minutes) {
    val nowTime = OffsetDateTime.now(zoneId)
    nowTime.getHour match {
      case 0 => // 凌晨留出一个小时，后期中央节点维护窗口
      case _ =>
        val startTime = nowTime.toInstant.toEpochMilli
        fetchMode match {
          case "FetchModeCH" =>
            Future.sequence {
              observersCache.zipWithIndex.map { i =>
                val idx = i._2 % 5
                (fetchers(idx) ? FetchModeCH(i._1)).mapTo[Option[(Int, Int)]]
              }
            }(implicitly, ecBlocking).map { result =>
              val costTime = OffsetDateTime.now(zoneId).toInstant.toEpochMilli - startTime
              val totalNum = result.length
              val errNum = result.count(i => i.isEmpty)
              val zeroNum = result.count(i => i.isDefined && i.get._1 == 0)
              val totalMiner = result.map{ i =>
                val ai = i.getOrElse((0, 0))
                ai._1 + ai._2
              }.sum
              val aliveMiner = result.map(_.getOrElse((0, 0))._1).sum
              val timestamp = OffsetDateTime.now(zoneId)
              recorder ! Record.LogTasks("", costTime, totalNum, errNum, zeroNum, totalMiner, aliveMiner, timestamp)

              (recorder ? Record.FetchLogs(nowTime)).mapTo[Seq[(String, OffsetDateTime)]].map { fetchLogs =>
                apiService.syncFetchMonitorToRemote(fetchLogs.map(_._1))
              }
            }

          case _ =>
        }
    }
  }

  def testFetch() = {
    val startTime = OffsetDateTime.now(zoneId).toInstant.toEpochMilli
    Future.sequence {
      fetchService.testObservers.zipWithIndex.map { i =>
        val idx = i._2 % 5
        (fetchers(idx) ? FetchModeCH(("", i._1))).mapTo[Option[(Int, Int)]]
      }
    }(implicitly, ecBlocking).map {
      result =>
        println(result)
        val costTime = OffsetDateTime.now(zoneId).toInstant.toEpochMilli - startTime
        println(s"CostTime: $costTime ms")
        result
    }
  }

  def queryLogs(lastTime: OffsetDateTime): Future[(Seq[(String, OffsetDateTime)], Seq[(String, OffsetDateTime)], Seq[(String, OffsetDateTime)])] = {
    (recorder ? Record.Logs(lastTime)).mapTo[(Seq[(String, OffsetDateTime)], Seq[(String, OffsetDateTime)], Seq[(String, OffsetDateTime)])]
  }

  def queryObservers(searchName: Option[String], searchObsv: Option[String]) = {
    observersCache.filter { i =>
      var result = false
      if (searchName.isDefined) {
        result = i._1.contains(searchName.get)
      }
      if (searchObsv.isDefined) {
        result = result && i._2.contains(searchObsv.get)
      }
      result
    }
  }

  def addAllObservers(observers: Array[(String, String)]) = {
    apiService.addAllObservers(observers).map {
      case true =>
        observersCache = (observersCache.toSet ++ observers).toList
        true
      case false =>
        false
    }
  }

  def addOneObserver(observer: (String, String)): Future[Boolean] = {
    apiService.addOneObserver(observer).map {
      case true =>
        observersCache = observersCache :+ observer
        true
      case false =>
        false
    }
  }

  def removeOneObserver(observer: (String, String)): Future[Boolean] = {
    apiService.removeOneObserver(observer).map {
      case true =>
        observersCache = observersCache.filterNot(i => i._1 == observer._1 && i._2 == observer._2)
        true
      case false =>
        false
    }
  }

  def updateOneObserver(observerOld: (String, String), observerNew: (String, String)): Future[Boolean] = {
    apiService.updateOneObserver(observerOld, observerNew).map {
      case true =>
        val idx = observersCache.indexWhere(i => i._1 == observerOld._1 && i._2 == observerOld._2)
        if (idx >= 0) {
          observersCache = observersCache.updated(idx, observerNew)
        }
        true
      case false =>
        false
    }
    Future.successful(true)
  }

  def initAllObserversFromApi(): Future[Unit] = {
    apiService.fetchAllObservers().map { all =>
      observersCache = all
    }
  }

}
