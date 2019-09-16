package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, ControllerComponents}
import services.{ApiService, FetchCenter}

import scala.concurrent.ExecutionContext

@Singleton
class RemoteController @Inject()(cc: ControllerComponents,
                                 fetchCenter: FetchCenter,
                                 apiService: ApiService,
                                 implicit val executionContext: ExecutionContext
                                ) extends AbstractController(cc) {



}
