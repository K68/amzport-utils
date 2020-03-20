package services

import monix.execution.Ack.{Continue, Stop}
import monix.execution.{Ack, Cancelable}
import monix.execution.cancelables.SingleAssignCancelable
import monix.reactive.{Observable, Observer}
import monix.reactive.OverflowStrategy.Unbounded
import monix.reactive.observers.Subscriber
import monix.execution.Scheduler.{global => scheduler}
import scala.concurrent.duration._
import scala.concurrent.Future

object ObserverManager {

  case class SmsEntity(pn: String, msg: String)

  private val observable: Observable[SmsEntity] = Observable.create[SmsEntity](Unbounded) { subscriber =>
    val c = SingleAssignCancelable()
    subscriberOpt = Some(subscriber)
    c := Cancelable(() => "")
  }

  private var subscriberOpt: Option[Subscriber.Sync[SmsEntity]] = None

  def subscribe(cb: SmsEntity => Boolean): Cancelable = {
    val observer: Observer[SmsEntity] = new Observer[SmsEntity] {
      def onNext(elem: SmsEntity): Future[Ack] = {
        if (cb(elem)) {
          Continue
        } else {
          Stop
        }
      }
      def onError(ex: Throwable): Unit = ex.printStackTrace()

      override def onComplete(): Unit = println("O completed")
    }

    observable.throttle(500.milliseconds, 1).subscribe(Subscriber(observer, scheduler))
  }

  def notify(smsEntity: SmsEntity): Option[String] = {
    if (subscriberOpt.isDefined) {
      subscriberOpt.get.onNext(smsEntity)
      Some("")
    } else {
      None
    }
  }

}
