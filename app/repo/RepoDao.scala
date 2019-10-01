package repo
import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import java.time.OffsetDateTime
import com.github.tminglei.slickpg.LTree
import RepoCase._
@Singleton
class RepoDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import repo.MyPostgresDriver.api._
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** GetResult implicit for fetching ApiAssetsRow objects using plain SQL queries */
  implicit def GetResultApiAssetsRow(implicit e0: GR[Int], e1: GR[OffsetDateTime], e2: GR[String]): GR[ApiAssetsRow] = GR{
    prs => import prs._
    ApiAssetsRow.tupled((<<[Int], <<[Int], <<[OffsetDateTime], <<[String]))
  }
  /** Table description of table api_assets. Objects of this class serve as prototypes for rows in queries. */
  class ApiAssets(_tableTag: Tag) extends profile.api.Table[ApiAssetsRow](_tableTag, "api_assets") {
    def * = (id, userId, updateTime, keyValue) <> (ApiAssetsRow.tupled, ApiAssetsRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(userId), Rep.Some(updateTime), Rep.Some(keyValue))).shaped.<>({r=>import r._; _1.map(_=> ApiAssetsRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column user_id SqlType(int4) */
    val userId: Rep[Int] = column[Int]("user_id")
    /** Database column update_time SqlType(timestamptz) */
    val updateTime: Rep[OffsetDateTime] = column[OffsetDateTime]("update_time")
    /** Database column key_value SqlType(varchar), Length(96,true) */
    val keyValue: Rep[String] = column[String]("key_value", O.Length(96,varying=true))

    /** Uniqueness Index over (keyValue) (database name api_assets_key_value_uindex) */
    val index1 = index("api_assets_key_value_uindex", keyValue, unique=true)
  }
  /** Collection-like TableQuery object for table ApiAssets */
  lazy val ApiAssets = new TableQuery(tag => new ApiAssets(tag))
  
  object ApiAssetsAuto {
      def all(): Future[Seq[ApiAssetsRow]] = db.run {
        ApiAssets.result
      }
      def getById(id: Int): Future[ApiAssetsRow] = db.run {
        ApiAssets.filter(_.id === id).result.head
      }
      def getByIdOption(id: Int): Future[Option[ApiAssetsRow]] = db.run {
        ApiAssets.filter(_.id === id).result.headOption
      }
      def insert(item: ApiAssetsRow): Future[Int] = db.run {
        ApiAssets returning ApiAssets.map(_.id) += item
      }
      def update(item: ApiAssetsRow): Future[Int] = db.run {
        ApiAssets.filter(_.id === item.id).update(item)
      }
      def upsert(item: ApiAssetsRow): Future[Int] = db.run {
        ApiAssets.insertOrUpdate(item)
      }
      def exists(id : Int) : Future[Boolean] = db.run {
        ApiAssets.filter(_.id === id).exists.result
      }
      def delete(id: Int): Future[Unit] = db.run(ApiAssets.filter(_.id === id).delete).map(_ => ())
    }
                   


  /** GetResult implicit for fetching MonitorRow objects using plain SQL queries */
  implicit def GetResultMonitorRow(implicit e0: GR[Int], e1: GR[OffsetDateTime]): GR[MonitorRow] = GR{
    prs => import prs._
    MonitorRow.tupled((<<[Int], <<[Int], <<[Int], <<[Int], <<[OffsetDateTime], <<[Int], <<[Int]))
  }
  /** Table description of table monitor. Objects of this class serve as prototypes for rows in queries. */
  class Monitor(_tableTag: Tag) extends profile.api.Table[MonitorRow](_tableTag, "monitor") {
    def * = (id, observerId, userId, apiAssetsId, timeshoot, liveOnline, totalNum) <> (MonitorRow.tupled, MonitorRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(observerId), Rep.Some(userId), Rep.Some(apiAssetsId), Rep.Some(timeshoot), Rep.Some(liveOnline), Rep.Some(totalNum))).shaped.<>({r=>import r._; _1.map(_=> MonitorRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column observer_id SqlType(int4) */
    val observerId: Rep[Int] = column[Int]("observer_id")
    /** Database column user_id SqlType(int4) */
    val userId: Rep[Int] = column[Int]("user_id")
    /** Database column api_assets_id SqlType(int4) */
    val apiAssetsId: Rep[Int] = column[Int]("api_assets_id")
    /** Database column timeshoot SqlType(timestamptz) */
    val timeshoot: Rep[OffsetDateTime] = column[OffsetDateTime]("timeshoot")
    /** Database column live_online SqlType(int4) */
    val liveOnline: Rep[Int] = column[Int]("live_online")
    /** Database column total_num SqlType(int4) */
    val totalNum: Rep[Int] = column[Int]("total_num")
  }
  /** Collection-like TableQuery object for table Monitor */
  lazy val Monitor = new TableQuery(tag => new Monitor(tag))
  
  object MonitorAuto {
      def all(): Future[Seq[MonitorRow]] = db.run {
        Monitor.result
      }
      def getById(id: Int): Future[MonitorRow] = db.run {
        Monitor.filter(_.id === id).result.head
      }
      def getByIdOption(id: Int): Future[Option[MonitorRow]] = db.run {
        Monitor.filter(_.id === id).result.headOption
      }
      def insert(item: MonitorRow): Future[Int] = db.run {
        Monitor returning Monitor.map(_.id) += item
      }
      def update(item: MonitorRow): Future[Int] = db.run {
        Monitor.filter(_.id === item.id).update(item)
      }
      def upsert(item: MonitorRow): Future[Int] = db.run {
        Monitor.insertOrUpdate(item)
      }
      def exists(id : Int) : Future[Boolean] = db.run {
        Monitor.filter(_.id === id).exists.result
      }
      def delete(id: Int): Future[Unit] = db.run(Monitor.filter(_.id === id).delete).map(_ => ())
    }
                   


  /** GetResult implicit for fetching ObserverRow objects using plain SQL queries */
  implicit def GetResultObserverRow(implicit e0: GR[Int], e1: GR[String], e2: GR[OffsetDateTime]): GR[ObserverRow] = GR{
    prs => import prs._
    ObserverRow.tupled((<<[Int], <<[Int], <<[Int], <<[String], <<[String], <<[OffsetDateTime], <<[String]))
  }
  /** Table description of table observer. Objects of this class serve as prototypes for rows in queries. */
  class Observer(_tableTag: Tag) extends profile.api.Table[ObserverRow](_tableTag, "observer") {
    def * = (id, userId, apiAssetsId, nickname, observeUrl, updateTime, openLink) <> (ObserverRow.tupled, ObserverRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(userId), Rep.Some(apiAssetsId), Rep.Some(nickname), Rep.Some(observeUrl), Rep.Some(updateTime), Rep.Some(openLink))).shaped.<>({r=>import r._; _1.map(_=> ObserverRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column user_id SqlType(int4) */
    val userId: Rep[Int] = column[Int]("user_id")
    /** Database column api_assets_id SqlType(int4) */
    val apiAssetsId: Rep[Int] = column[Int]("api_assets_id")
    /** Database column nickname SqlType(varchar), Length(20,true) */
    val nickname: Rep[String] = column[String]("nickname", O.Length(20,varying=true))
    /** Database column observe_url SqlType(varchar), Length(96,true) */
    val observeUrl: Rep[String] = column[String]("observe_url", O.Length(96,varying=true))
    /** Database column update_time SqlType(timestamptz) */
    val updateTime: Rep[OffsetDateTime] = column[OffsetDateTime]("update_time")
    /** Database column open_link SqlType(varchar), Length(32,true) */
    val openLink: Rep[String] = column[String]("open_link", O.Length(32,varying=true))
  }
  /** Collection-like TableQuery object for table Observer */
  lazy val Observer = new TableQuery(tag => new Observer(tag))
  
  object ObserverAuto {
      def all(): Future[Seq[ObserverRow]] = db.run {
        Observer.result
      }
      def getById(id: Int): Future[ObserverRow] = db.run {
        Observer.filter(_.id === id).result.head
      }
      def getByIdOption(id: Int): Future[Option[ObserverRow]] = db.run {
        Observer.filter(_.id === id).result.headOption
      }
      def insert(item: ObserverRow): Future[Int] = db.run {
        Observer returning Observer.map(_.id) += item
      }
      def update(item: ObserverRow): Future[Int] = db.run {
        Observer.filter(_.id === item.id).update(item)
      }
      def upsert(item: ObserverRow): Future[Int] = db.run {
        Observer.insertOrUpdate(item)
      }
      def exists(id : Int) : Future[Boolean] = db.run {
        Observer.filter(_.id === id).exists.result
      }
      def delete(id: Int): Future[Unit] = db.run(Observer.filter(_.id === id).delete).map(_ => ())
    }
                   


  /** GetResult implicit for fetching UserRow objects using plain SQL queries */
  implicit def GetResultUserRow(implicit e0: GR[Int], e1: GR[String], e2: GR[OffsetDateTime]): GR[UserRow] = GR{
    prs => import prs._
    UserRow.tupled((<<[Int], <<[Int], <<[String], <<[String], <<[Int], <<[OffsetDateTime]))
  }
  /** Table description of table user. Objects of this class serve as prototypes for rows in queries. */
  class User(_tableTag: Tag) extends profile.api.Table[UserRow](_tableTag, "user") {
    def * = (id, userType, loginName, loginPassword, apiKeyLimit, lastLoginTime) <> (UserRow.tupled, UserRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(userType), Rep.Some(loginName), Rep.Some(loginPassword), Rep.Some(apiKeyLimit), Rep.Some(lastLoginTime))).shaped.<>({r=>import r._; _1.map(_=> UserRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column user_type SqlType(int4) */
    val userType: Rep[Int] = column[Int]("user_type")
    /** Database column login_name SqlType(varchar), Length(20,true) */
    val loginName: Rep[String] = column[String]("login_name", O.Length(20,varying=true))
    /** Database column login_password SqlType(varchar), Length(32,true) */
    val loginPassword: Rep[String] = column[String]("login_password", O.Length(32,varying=true))
    /** Database column api_key_limit SqlType(int4) */
    val apiKeyLimit: Rep[Int] = column[Int]("api_key_limit")
    /** Database column last_login_time SqlType(timestamptz) */
    val lastLoginTime: Rep[OffsetDateTime] = column[OffsetDateTime]("last_login_time")

    /** Uniqueness Index over (loginName) (database name user_login_name_uindex) */
    val index1 = index("user_login_name_uindex", loginName, unique=true)
  }
  /** Collection-like TableQuery object for table User */
  lazy val User = new TableQuery(tag => new User(tag))
  
  object UserAuto {
      def all(): Future[Seq[UserRow]] = db.run {
        User.result
      }
      def getById(id: Int): Future[UserRow] = db.run {
        User.filter(_.id === id).result.head
      }
      def getByIdOption(id: Int): Future[Option[UserRow]] = db.run {
        User.filter(_.id === id).result.headOption
      }
      def insert(item: UserRow): Future[Int] = db.run {
        User returning User.map(_.id) += item
      }
      def update(item: UserRow): Future[Int] = db.run {
        User.filter(_.id === item.id).update(item)
      }
      def upsert(item: UserRow): Future[Int] = db.run {
        User.insertOrUpdate(item)
      }
      def exists(id : Int) : Future[Boolean] = db.run {
        User.filter(_.id === id).exists.result
      }
      def delete(id: Int): Future[Unit] = db.run(User.filter(_.id === id).delete).map(_ => ())
    }
                   
}

object RepoCase{
implicit val ltreeFormat: OFormat[LTree] = Json.format[LTree]
/** Entity class storing rows of table ApiAssets
 *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
 *  @param userId Database column user_id SqlType(int4)
 *  @param updateTime Database column update_time SqlType(timestamptz)
 *  @param keyValue Database column key_value SqlType(varchar), Length(96,true) */
case class ApiAssetsRow(id: Int, userId: Int, updateTime: OffsetDateTime, keyValue: String)
implicit val ApiAssetsRowFormat: OFormat[ApiAssetsRow] = Json.format[ApiAssetsRow]

/** Entity class storing rows of table Monitor
 *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
 *  @param observerId Database column observer_id SqlType(int4)
 *  @param userId Database column user_id SqlType(int4)
 *  @param apiAssetsId Database column api_assets_id SqlType(int4)
 *  @param timeshoot Database column timeshoot SqlType(timestamptz)
 *  @param liveOnline Database column live_online SqlType(int4)
 *  @param totalNum Database column total_num SqlType(int4) */
case class MonitorRow(id: Int, observerId: Int, userId: Int, apiAssetsId: Int, timeshoot: OffsetDateTime, liveOnline: Int, totalNum: Int)
implicit val MonitorRowFormat: OFormat[MonitorRow] = Json.format[MonitorRow]

/** Entity class storing rows of table Observer
 *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
 *  @param userId Database column user_id SqlType(int4)
 *  @param apiAssetsId Database column api_assets_id SqlType(int4)
 *  @param nickname Database column nickname SqlType(varchar), Length(20,true)
 *  @param observeUrl Database column observe_url SqlType(varchar), Length(96,true)
 *  @param updateTime Database column update_time SqlType(timestamptz)
 *  @param openLink Database column open_link SqlType(varchar), Length(32,true) */
case class ObserverRow(id: Int, userId: Int, apiAssetsId: Int, nickname: String, observeUrl: String, updateTime: OffsetDateTime, openLink: String)
implicit val ObserverRowFormat: OFormat[ObserverRow] = Json.format[ObserverRow]

/** Entity class storing rows of table User
 *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
 *  @param userType Database column user_type SqlType(int4)
 *  @param loginName Database column login_name SqlType(varchar), Length(20,true)
 *  @param loginPassword Database column login_password SqlType(varchar), Length(32,true)
 *  @param apiKeyLimit Database column api_key_limit SqlType(int4)
 *  @param lastLoginTime Database column last_login_time SqlType(timestamptz) */
case class UserRow(id: Int, userType: Int, loginName: String, loginPassword: String, apiKeyLimit: Int, lastLoginTime: OffsetDateTime)
implicit val UserRowFormat: OFormat[UserRow] = Json.format[UserRow]
}
