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

    dbOps.selectDistribution  .map(set => set.collect{ case IndexedSeq(t, p, _*) =>(t-> p) }.filter(_._2.toFloat > 2) )
                              .map(seq => seq.collect{ case Tuple2(t, p) => JsObject(Seq( ("Type"-> JsString(t)), ("Percentage"->JsString(p)) ) )} )
                              .map( (jsObjects) => Ok(JsArray( jsObjects) ))
  }
  def checkArea(long: String, lat: String) = Action.async { request: (Request[AnyContent]) =>
    import models.Location

    val location = Location(long, lat)
    dbOps.checkLocation(location)
         .map((set: IndexedSeq[(String, String)]) => set.collect{ case Tuple2(t ,d) => JsObject(Seq (("Crime"->JsString(t)), ("Description" ->JsString(d)))) })
         .map(jsObjects => Ok(JsArray(jsObjects)) )


  }
  def getCrimeDeviation() = Action.async{

    dbOps.selectCrimeDev()   .map(seq => seq.collect{ case Tuple2(y, d) => JsObject(Seq (("Year"->JsString(y)), ("Percentage" ->JsString(d)))) })
                             .map(jsObjects => Ok(JsArray(jsObjects)))


  }

}
