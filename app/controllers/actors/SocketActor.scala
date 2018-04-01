package controllers.actors

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import models.Location
import play.api.libs.json.{JsArray, JsValue, Json, Writes}
import models.Crime._
import models.Crime
import controllers.Application
import services.postgres.{DatabaseConnection, MessageRepository}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object SocketActor {
  def props(out: ActorRef, dbOps: MessageRepository) = Props ( new SocketActor (out,dbOps))
}

class SocketActor (out: ActorRef, dbOps: MessageRepository) extends Actor{
  override def receive: Receive = {
    case location: JsValue => Some(Location (location("longitude").as[String], location("latitude").as[String]))
                                                .map( dbOps .checkLocation(_)
                                                .map((set) => set.collect{ case Tuple5(t , locDesc, desc, date, id) => Crime(t, desc, locDesc, date, id)})
                                                .map( seq => seq.map(Json.toJson(_)))
                                                .flatMap(jsObjects => Future { JsArray(jsObjects) } )
                                                .onComplete(out ! _))
    case _  => out ! "Not known"
  }}
