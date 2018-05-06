package controllers

import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy}
import controllers.actors.{ClusteringActor, SocketActor}
import models.Crime._
import models.{Crime, Location}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import services.postgres.{AnormRepo, DatabaseConnection}
import bayes.Classifiers
import com.github.mauricio.async.db.RowData

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class Application @Inject()(cc: ControllerComponents)(anorm: AnormRepo, postgres: DatabaseConnection, classifier: Classifiers)(implicit exec: ExecutionContext)   extends AbstractController(cc) {

  implicit val actorSystem = ActorSystem()
  implicit val actorMaterializer = ActorMaterializer()
  implicit val crimeWrites = Crime.crimeWrites

  val dbOps = postgres.messagesRepository

  /** WebSocket handled by actors*/
  def socket = WebSocket.accept[JsValue,JsValue] {_ => ActorFlow.actorRef( { out: ActorRef => SocketActor.props(out, dbOps) }) }

  /** Using Anorm */
  def getAll () = Action.async{ Future { anorm.getAll().map( Json.toJson(_) ) }.map(JsArray(_)).map(Ok(_)) }

  def clustering(latitude: String, longitude: String, dist: Long, crimeType: String, events: Long) = Action.async{
    Future {
      val location = Location(latitude, longitude)
      Logger.info(s"Searching for cluster near $location of type $crimeType ...")
      val crimeList = anorm.clusterScan( location, dist, crimeType, events )
      val size = crimeList.groupBy(_.cluster_id).size;
      Logger.warn(s" $size cluster[s] found of type $crimeType")
      crimeList.map(Json.toJson(_))
    }.map(JsArray(_)).map(Ok(_))
  }


  /** REST controllers */
  def setPieChart () = Action.async{ request : (Request[AnyContent]) =>

    dbOps.selectTotal_Arrests_Domestics()
         .map( queryResult => queryResult.rows.get.map( row =>  row(0).asInstanceOf[String] -> JsNumber( row(1).asInstanceOf[Long]) ))
         .map(( seq => Ok(JsObject( seq )) ))
  }

  def setLineChart (limit :Int) = Action.async{
    dbOps.selectTypeSeries()
         .map(queryResult => Json.obj(  "timeseries" ->
           queryResult.rows.get
                      .groupBy(row => row(0).asInstanceOf[String])
                      .toList
                      .filter(_._2.map(_(2).asInstanceOf[Long]).reduce(_+_) > limit)
                      .map((tuple: (String, IndexedSeq[RowData])) => Json.obj("type" -> tuple._1, "data" -> JsArray( tuple._2.map(row => (Json.obj("year" -> JsNumber(row(1).asInstanceOf[Int]), "count" -> JsNumber(row(2).asInstanceOf[Long])))))))

         ))

      .map((result) => Ok(result))
  }


  def clusterService = WebSocket.accept[JsValue, JsValue] {_ => ActorFlow.actorRef( { out: ActorRef => ClusteringActor.props(out, dbOps) })}

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
          .map((set) => set.collect{ case Tuple5(t , locDesc, desc, date, id) => Crime(t, locDesc, date, id.toLong, description = desc)})
          .map( seq => seq.map(Json.toJson(_)))
          .flatMap(jsObjects => Future { Ok(JsArray(jsObjects)) } )

  }

  def getCrimeDeviation() = Action.async{
    Logger.info("Making query for crime deviation")
    dbOps.selectCrimeDev()   .map(seq => seq.collect{ case Tuple2(y, d) => JsObject(Seq (("Year"->JsString(y)), ("Percentage" ->JsString(d)))) })
                             .map(jsObjects => { Logger.info("Query succesfull "); Ok(JsArray(jsObjects)) })
  }

  def predict(crime: String, location:String) = Action.async{ _ =>
    classifier.predict(crime, location).map(response  => Ok( response.getOrElse( Json.obj("Error" -> JsString("Classifier not ready"))) ))
  }

}
