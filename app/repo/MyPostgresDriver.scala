package repo

import com.github.tminglei.slickpg._
import play.api.libs.json.{JsValue, Json}
import slick.basic.Capability
import java.time.OffsetDateTime

import com.github.tminglei.slickpg.window.PgWindowFuncSupport

trait MyPostgresDriver
    extends ExPostgresProfile
      with PgArraySupport
      with PgDate2Support
      with PgPlayJsonSupport
      with PgWindowFuncSupport
      with PgLTreeSupport {
  def pgjson =
    "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api: API = new API {}

  trait API
      extends super.API
        with ArrayImplicits
        with DateTimeImplicits
        with JsonImplicits
        with LTreeImplicits {
    implicit val strListTypeMapper: DriverJdbcType[List[String]] =
      new SimpleArrayJdbcType[String]("varchar").to(_.toList)

    implicit val offTsListTypeMapper: DriverJdbcType[List[OffsetDateTime]] =
      new AdvancedArrayJdbcType[OffsetDateTime](
        "timestamptz",
        s =>
          utils.SimpleArrayUtils
            .fromString[OffsetDateTime](i =>
              OffsetDateTime.parse(i, date2TzDateTimeFormatter))(s)
            .orNull,
        v =>
          utils.SimpleArrayUtils.mkString[OffsetDateTime](
            _.format(date2TzDateTimeFormatter))(v)
      ).to(_.toList)
    implicit val playJsonArrayTypeMapper: DriverJdbcType[List[JsValue]] =
      new AdvancedArrayJdbcType[JsValue](
        pgjson,
        s =>
          utils.SimpleArrayUtils
            .fromString[JsValue](i => Json.parse(i))(s)
            .orNull,
        v => utils.SimpleArrayUtils.mkString[JsValue](_.toString)(v))
        .to(_.toList)
  }
}

object MyPostgresDriver extends MyPostgresDriver
