package actors

import java.time.OffsetDateTime
import akka.actor.{Actor, Props}

object Record {
  def props(): Props = Props(new Record())

  case class LogFetch(
                       obsvName: String,
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
}

class Record extends Actor {
  import Record._

  def receive: PartialFunction[Any, Unit] = {
    case LogFetch(obsvName, costTime, total, alive, timestamp) =>

    case LogTasks(taskName, costTime, totalNum, errNum, zeroNum, totalMiner, aliveMiner, timestamp) =>

    case LogApiSync(syncNum, syncSuccess, syncLog, timestamp) =>

  }

}
