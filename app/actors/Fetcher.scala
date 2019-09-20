package actors

import java.time.{OffsetDateTime, ZoneId}

import akka.actor.{Actor, Props}
import services.FetchCenter

object Fetcher {
  def props(center: FetchCenter): Props = Props(new Fetcher(center))

  case class FetchModeCH(observer: (String, String))
}

class Fetcher(fc: FetchCenter) extends Actor {
  import Fetcher._
  val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")

  def receive: PartialFunction[Any, Unit] = {
    case FetchModeCH(observer) =>
      val startTime = OffsetDateTime.now().toInstant.toEpochMilli
      val fetchOne = fc.fetchService.fetchOneObserver(observer._2)
      val costTime = OffsetDateTime.now().toInstant.toEpochMilli - startTime
      val toSend = fc.fetchService.parseFetchResult(fetchOne)
      if (toSend.isDefined) {
        fc.recorder ! Record.LogFetch(observer._1, observer._2, costTime, toSend.get._1 + toSend.get._2, toSend.get._1, OffsetDateTime.now(zoneId))
      } else {
        fc.recorder ! Record.LogFetch(observer._1, observer._2, costTime, 0, 0, OffsetDateTime.now(zoneId))
      }
      sender() ! toSend
  }

}
