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

@Singleton
class FetchCenter @Inject() (appLifecycle: ApplicationLifecycle,
                             actorSystem: ActorSystem,
                             val fetchService: FetchService,
                             implicit val executionContext: ExecutionContext
                            ) {
  val fetchers = List.range(1, 6).map(i => actorSystem.actorOf(Fetcher.props(this), s"fetcher_$i"))

  implicit val timeout: Timeout = 3600.seconds

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
    }
  }
}
