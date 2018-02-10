package controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.postgres.DatabaseConnection

import scala.concurrent.{ExecutionContext, Future}


/*TODO use Promise wtih actors*/

@Singleton
class Application @Inject()(cc: ControllerComponents)(postgres: DatabaseConnection)(implicit exec: ExecutionContext)   extends AbstractController(cc) {

  val dbOps = postgres.messagesRepository

  def listCrimeTypeDistribution = Action.async {

    Logger.info("Making a query for crime type distribution")
    dbOps.selectDistribution  .map(set => set.collect{ case IndexedSeq(t, p, _*) =>(t-> p) }.filter(_._2.toFloat > 2) )
                              .map(seq => seq.collect{ case Tuple2(t, p) => JsObject(Seq( ("Type"-> JsString(t)), ("Percentage"->JsString(p)) ) )} )
                              .flatMap( (jsObjects) => Future{ Ok(JsArray( jsObjects) ) })
  }
  def checkArea(long: String, lat: String) = Action.async {
    import models.Location

    val location = Location(long, lat)
    Logger.info(s"Checking location $location")

    dbOps .checkLocation(location)
          .map((set) => set.collect{ case Tuple5(t , locDesc, desc, date, id) => JsObject(Seq (("Crime"->JsString(t)), ("Description" ->JsString(desc)), ("LocationDescription"->JsString(locDesc)), ("Date"->JsString(date)),("ID"->JsString(id))) )})
          .flatMap(jsObjects => Future { Ok(JsArray(jsObjects)) } )

  }
  def getCrimeDeviation() = Action.async{
    Logger.info("Making query for crime deviation")
    dbOps.selectCrimeDev()   .map(seq => seq.collect{ case Tuple2(y, d) => JsObject(Seq (("Year"->JsString(y)), ("Percentage" ->JsString(d)))) })
                             .map(jsObjects => { Logger.info("Query succesfull "); Ok(JsArray(jsObjects)) })
  }

}
