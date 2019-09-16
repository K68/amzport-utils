package services

import java.time.OffsetDateTime

import actors.Fetcher
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

  implicit val timeout: Timeout = 3600.seconds

  val fetchMode = configuration.underlying.getString("FetchMode")

  var observersCache = Array[(String, String)]()

  def testFetch() = {
    val startTime = OffsetDateTime.now().toInstant.toEpochMilli
    Future.sequence(
      fetchService.testObservers.zipWithIndex.map{ i =>
        val idx = i._2 % 5
        (fetchers(idx) ? FetchModeCH(i._1)).mapTo[Option[(Int, Int)]]
      }
    ).map {
      result =>
        println(result)
        val costTime = OffsetDateTime.now().toInstant.toEpochMilli - startTime
        println(s"CostTime: $costTime ms")
        result
    }
  }

  def updateAllObservers(observers: Array[(String, String)]) = {
    apiService.updateAllObservers(observers).map {
      case true =>
        observersCache = observers
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
          observersCache.update(idx, observerNew)
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
