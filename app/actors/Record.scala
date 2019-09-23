package actors

import java.time.OffsetDateTime
import akka.actor.{Actor, Props}

object Record {
  def props(): Props = Props(new Record())

  case class LogFetch(
                       obsvName: String,
                       obsvUrl: String,
                       costTime: Long,
                       total: Long,
                       alive: Long,
                       timestamp: OffsetDateTime
                     )
  case class LogTasks(
                       taskName: String,
                       costTime: Long,
                       totalNum: Long,              // 全部处理的观察者链接数量
                       errNum: Long,                // 出错的观察者链接数量
                       zeroNum: Long,               // 激活量为0的观察者链接数量
                       totalMiner: Long,            // 所有的资产总计数
                       aliveMiner: Long,            // 激活的资产总计数
                       timestamp: OffsetDateTime
                     )
  case class LogApiSync(
                         syncNum: Long,             // 同步到云端的有效观察者数据数量
                         syncSuccess: Boolean,      // 同步是否成功
                         syncLog: String,           // 同步反馈记录
                         timestamp: OffsetDateTime,
                       )
  case class Logs(
                 lastTime: OffsetDateTime,
                 )
  case class FetchLogs(
                        lastTime: OffsetDateTime,
                      )
}

class Record extends Actor {
  import Record._
  import scala.collection.mutable

  val LOG_MAX_LENGTH = 3000

  val logFetch: mutable.Queue[(String, OffsetDateTime)] = mutable.Queue.empty[(String, OffsetDateTime)]
  val logTasks: mutable.Queue[(String, OffsetDateTime)] = mutable.Queue.empty[(String, OffsetDateTime)]
  val logApiSync: mutable.Queue[(String, OffsetDateTime)] = mutable.Queue.empty[(String, OffsetDateTime)]

  def receive: PartialFunction[Any, Unit] = {
    case LogFetch(obsvName, obsvUrl, costTime, total, alive, timestamp) =>
      val v = s"$obsvName,$obsvUrl,$costTime,$total,$alive,$timestamp"
      logFetch += ((v, timestamp))
      if (logFetch.length > LOG_MAX_LENGTH) {
        logFetch.dequeue()
      }

    case LogTasks(taskName, costTime, totalNum, errNum, zeroNum, totalMiner, aliveMiner, timestamp) =>
      val v = s"$taskName,$costTime,$totalNum,$errNum,$zeroNum,$totalMiner,$aliveMiner,$timestamp"
      logTasks += ((v, timestamp))
      if (logTasks.length > LOG_MAX_LENGTH) {
        logTasks.dequeue()
      }

    case LogApiSync(syncNum, syncSuccess, syncLog, timestamp) =>
      val v = s"$syncNum,$syncSuccess,$syncLog,$timestamp"
      logApiSync += ((v, timestamp))
      if (logApiSync.length > LOG_MAX_LENGTH) {
        logApiSync.dequeue()
      }

    case Logs(lastTime) =>
      sender() ! (logFetch.filter(_._2.isAfter(lastTime)),
        logTasks.filter(_._2.isAfter(lastTime)),
        logApiSync.filter(_._2.isAfter(lastTime)))

    case FetchLogs(lastTime) =>
      sender() ! logFetch.filter(_._2.isAfter(lastTime))

  }

}
