package repo

import slick.codegen.SourceCodeGenerator
import slick.jdbc.JdbcProfile
import slick.sql.SqlProfile.ColumnOption

import scala.collection.Set
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object CodeGeneration extends App {
  run(
    "repo.MyPostgresDriver",
    "org.postgresql.Driver",
    "jdbc:postgresql://127.0.0.1:5432/mol?user=postgres&password=666666",
    new java.io.File("app").getCanonicalPath,
    "repo"
  )

  def run(slickDriver: String,
          jdbcDriver: String,
          url: String,
          outputDir: String,
          pkg: String): Unit = {
    val driver: JdbcProfile =
      Class
        .forName(slickDriver + "$")
        .getField("MODULE$")
        .get(null)
        .asInstanceOf[JdbcProfile]
    val dbFactory = driver.api.Database
    val db = dbFactory.forURL(url,
                              driver = jdbcDriver,
                              user = null,
                              password = null,
                              keepAliveConnection = true)

    var caseClassCode: List[String] = List()
    var caseClassDoc: List[String] = List()

    val invalidTables = Set("spatial_ref_sys")
    val invalidCaseRow = Set("ClassLocationRow", "StidesRow")

    try {
      val m = Await.result(
        db.run(
          driver
            .createModel(None, ignoreInvalidDefaults = false)(
              ExecutionContext.global)
            .withPinnedSession),
        Duration.Inf)

      new SourceCodeGenerator(m.copy(tables = m.tables.filterNot(table => invalidTables(table.name.table)))) {
        override def packageCode(profile: String,
                                 pkg: String,
                                 container: String,
                                 parentType: Option[String]): String = {
          s"""
package $pkg
import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import java.time.OffsetDateTime
import com.github.tminglei.slickpg.LTree
import RepoCase._
@Singleton
class $container @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import repo.MyPostgresDriver.api._
  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
  ${indent(code)}
}
      """.trim() + "\n\nobject RepoCase{\nimplicit val ltreeFormat: OFormat[LTree] = Json.format[LTree]\n" + (caseClassCode zip caseClassDoc)
            .map { case (_code, _doc) => super.docWithCode(_doc, _code) }
            .mkString("\n\n") + "\n}\n"
        }
        // disable DDL generation
        override val ddlEnabled = false
        // override table generator
        override def Table = new Table(_) {
          override def Column = new Column(_) { column =>
            // customize db type -> scala type mapping, pls adjust it according to your environment
            override def rawType: String = model.tpe match {
              case "play.api.libs.json.JsValue"        => "JsValue"
              case "java.sql.Timestamp"                => "OffsetDateTime"
              case "com.github.tminglei.slickpg.LTree" => "LTree"
              // currently, all types that's not built-in support were mapped to `String`
              case "String" =>
                model.options
                  .find(_.isInstanceOf[ColumnOption.SqlType])
                  .map(_.asInstanceOf[ColumnOption.SqlType].typeName)
                  .map({
                    case "varchar"      => "String"
                    case "_varchar"     => "List[String]"
                    case "_jsonb"       => "List[JsValue]"
//                    case "geometry"     => "Geometry"
                    case "_timestamptz" => "List[OffsetDateTime]"
                    case something => {
                      println("Type isn't match flag 1: " + something)
                      "String"
                    }
                  })
                  .getOrElse("String")
              // currently, all types that's array were mapped to `scala.collection.Seq`
              case "scala.collection.Seq" =>
                model.options
                  .find(_.isInstanceOf[ColumnOption.SqlType])
                  .map(_.asInstanceOf[ColumnOption.SqlType].typeName)
                  .map {
                    case "_int4"   => "List[Int]"
                    case "_float4" => "List[Float]"
                    case "_int8"   => "List[Long]"
                    case "_float8" => "List[Double]"
                    case "_bool"   => "List[Boolean]"
                    case something => {
                      println("Type isn't match flag 2: " + something)
                      "List[Object]"
                    }
                  }
                  .getOrElse("List[Object]")
              // ltree list special
              case "scala.collection.immutable.List" =>
                model.options
                  .find(_.isInstanceOf[ColumnOption.SqlType])
                  .map(_.asInstanceOf[ColumnOption.SqlType].typeName)
                  .map {
                    case "_ltree" => "List[LTree]"
                    case something => {
                      println("Type isn't match flag 3: " + something)
                      "List[Object]"
                    }
                  }
                  .getOrElse("List[Object]")
              case _ => {
                super.rawType
              }
            }
          }
          override def EntityType = new EntityType {
            override def doc: String = {
              caseClassDoc = caseClassDoc :+ super.doc
              ""
            }
            override def code: String = {
              if(invalidCaseRow.exists(row => super.code.contains(row))) {
                caseClassCode = caseClassCode :+ super.code
              } else {
                caseClassCode = caseClassCode :+ super.code + s"\nimplicit val ${name}Format: OFormat[$name] = Json.format[$name]"
              }
              ""
            }
          }
          override def TableValue = new TableValue {
            var idType = "Int"
            override def code: String =
              super.code +
                s"""
                   |
                   |object ${name}Auto {
                   |    def all(): Future[Seq[${name}Row]] = db.run {
                   |      $name.result
                   |    }
                   |    def getById(id: $idType): Future[${name}Row] = db.run {
                   |      $name.filter(_.id === id).result.head
                   |    }
                   |    def getByIdOption(id: $idType): Future[Option[${name}Row]] = db.run {
                   |      $name.filter(_.id === id).result.headOption
                   |    }
                   |    def insert(item: ${name}Row): Future[$idType] = db.run {
                   |      $name returning $name.map(_.id) += item
                   |    }
                   |    def update(item: ${name}Row): Future[Int] = db.run {
                   |      $name.filter(_.id === item.id).update(item)
                   |    }
                   |    def upsert(item: ${name}Row): Future[Int] = db.run {
                   |      $name.insertOrUpdate(item)
                   |    }
                   |    def exists(id : $idType) : Future[Boolean] = db.run {
                   |      $name.filter(_.id === id).exists.result
                   |    }
                   |    def delete(id: $idType): Future[Unit] = db.run($name.filter(_.id === id).delete).map(_ => ())
                   |  }
                 """.stripMargin
          }
        }
      }.writeToFile(slickDriver, outputDir, pkg, "RepoDao", "RepoDao.scala")
    } finally db.close
  }

}
