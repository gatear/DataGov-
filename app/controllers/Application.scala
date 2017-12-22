package controllers

import javax.inject.{Inject, Singleton}

import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.mvc._
import play.api.mvc.{AbstractController, ControllerComponents}
import services.postgres.DatabaseConnection

import scala.concurrent.ExecutionContext

@Singleton
class Application @Inject()(cc: ControllerComponents)(postgres: DatabaseConnection)(implicit exec: ExecutionContext)   extends AbstractController(cc) {

  val dbOps = postgres.messagesRepository

  def listCrimeTypeDistribution = Action.async { request: (Request[AnyContent]) =>

    dbOps.selectDistribution().map(set => set.collect{ case IndexedSeq(t, p, _*) =>(t, p) } )
                              .map(seq => seq.collect{ case Tuple2(t, p) => JsObject(Seq( ("Type"-> JsString(t)), ("Percentage"->JsString(p)) ) )} )
                              .map( (jsObjects) => Ok(JsArray( jsObjects) ))
  }
}
