package services

import java.time.OffsetDateTime

import com.softwaremill.sttp._
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FetchService @Inject()(configuration: Configuration,
                             implicit val executionContext: ExecutionContext) {

  implicit val backend = HttpURLConnectionBackend()
  val statsFetchURL = configuration.underlying.getString("StatsFetchURL")

  val testObservers = List(
    "http://120.27.0.232/access/dashboard/?access_key=APIsXTOG8fIDqru5ehlmCpE",
    "http://120.27.0.232/access/dashboard/?access_key=API9C17fNPWk0xYvJHVSIrF",
    "http://120.27.0.232/access/dashboard/?access_key=API3dchoONbJF4eVC2DaiXE",
    "http://120.27.0.232/access/dashboard/?access_key=APIMVboAGavxtrSEeyHinpj",
    "http://120.27.0.232/access/dashboard/?access_key=APIP1F6LkSr0NafD2bRgEqJ",
    "http://120.27.0.232/access/dashboard/?access_key=APIFugPoQsC34MyEAGWVhNd",
    "http://120.27.0.232/access/dashboard/?access_key=APIvsQNqIUVhCcyPxrb0uop",
    "http://120.27.0.232/access/dashboard/?access_key=APIq6wOJW2K4eVbhkN1sQCz",
    "http://120.27.0.232/access/dashboard/?access_key=APIKxIhkECQgiFrlGO1A3jw",
    "http://120.27.0.232/access/dashboard/?access_key=API84XwYuGzmQhnpliUWsyK",
    "http://120.27.0.232/access/dashboard/?access_key=API7VEZx1G3vqOljDeJg9Ba",
    "http://120.27.0.232/access/dashboard/?access_key=API6sU5WIbeRrzfJluMndY3",
    "http://120.27.0.232/access/dashboard/?access_key=APISa7TFrx8skp4MZviPWX9",
    "http://120.27.0.232/access/dashboard/?access_key=APIaVhHcB0pkLfgNP8yYort",
    "http://120.27.0.232/access/dashboard/?access_key=APIN8273mDzhjAEKXnctRUV",
    "http://120.27.0.232/access/dashboard/?access_key=APIP9SruoBtVJbCDLzAG63Z",
    "http://120.27.0.232/access/dashboard/?access_key=APIlQ0DNiC9fpZS3GkWLcMj",
    "http://120.27.0.232/access/dashboard/?access_key=APIBykeYZTQrA5hiG1tKv2S",
    "http://120.27.0.232/access/dashboard/?access_key=API9faSHiPE4OJGUWVus8Zm",
    "http://120.27.0.232/access/dashboard/?access_key=APIt1zAjfugexviqBoFHOyP",
    "http://120.27.0.232/access/dashboard/?access_key=APIkc4Qdr9b7Zx1wL2HRIvi",
    "http://120.27.0.232/access/dashboard/?access_key=API4IrkOPxvs3t0VAdKbaWl",
    "http://120.27.0.232/access/dashboard/?access_key=APIJQqOdgV63fjo4A82WkCD",
    "http://120.27.0.232/access/dashboard/?access_key=APIAGuKlbT3cIvWj2HSQdmh",
    "http://120.27.0.232/access/dashboard/?access_key=API6BxJ0ZW3PQ4TNmfbjEtq",
    "http://120.27.0.232/access/dashboard/?access_key=APIMNcAI4v3DsX1bQd8l6qH",
    "http://120.27.0.232/access/dashboard/?access_key=APIV4tW9mANSQuKcInewvp2",
    "http://120.27.0.232/access/dashboard/?access_key=APItYJhyNeoUkaZjAxpLHg7",
    "http://120.27.0.232/access/dashboard/?access_key=APICHE2B4yc0fDVJjeRKpoA",
    "http://120.27.0.232/access/dashboard/?access_key=API5sSXKUtedAmvoLpQOx8H",
    "http://120.27.0.232/access/dashboard/?access_key=API2K5UhL3AzXZ9doRwbGQV",
    "http://120.27.0.232/access/dashboard/?access_key=API29EtFUp6D0I8dAPxG4zm",
    "http://120.27.0.232/access/dashboard/?access_key=APIiQzDX8BagYr9deCb6LmN",
    "http://120.27.0.232/access/dashboard/?access_key=APIynUV0vpYXDaCu8HFI17Z",
  )

  def fetchOneObserver(observer: String): Option[String] = {
    val req1 = sttp.get(uri"$observer")
    val rep1 = req1.send()
    val timestamp = OffsetDateTime.now().toInstant.toEpochMilli
    val req2 = sttp.get(uri"$statsFetchURL?_=$timestamp").cookies(rep1)
    val rep2 = req2.send()

    rep2.body match {
      case Right(x) => Some(x)
      case Left(e) =>
        println(s"FetchOneObserver Exception: $e")
        None
    }
  }

  def parseFetchResult(result: Option[String]): Option[(Int, Int)] = {
    result match {
      case Some(x) =>
        val r = Json.parse(x)
        val active = (r \ "data" \ "miners" \ "miner_active").asOpt[Int].getOrElse(-1)
        val inactive = (r \ "data" \ "miners" \ "miner_inactive").asOpt[Int].getOrElse(-1)
        Some(active, inactive)
      case None => None
    }
  }

}
