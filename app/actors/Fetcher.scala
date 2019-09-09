package actors

import akka.actor.{Actor, Props}
import services.FetchCenter

object Fetcher {
  def props(center: FetchCenter): Props = Props(new Fetcher(center))

  case class FetchModeCH(observer: String)
}

class Fetcher(fc: FetchCenter) extends Actor {
  import Fetcher._

  def receive: PartialFunction[Any, Unit] = {
    case FetchModeCH(observer) =>
      sender() ! fc.fetchService.parseFetchResult(fc.fetchService.fetchOneObserver(observer))
  }

}
