package controllers.actors

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import models.Location
import play.api.libs.json._
import models.Crime._
import models.Crime
import controllers.Application
import services.postgres.{DatabaseConnection, MessageRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

object ClusteringActor {

  /*Factory that returns the actors configuration*/
  def props(out: ActorRef, dbOps: MessageRepository) = Props ( new ClusteringActor (out,dbOps))
}

class ClusteringActor (out: ActorRef, dbOps: MessageRepository) extends Actor{

  override def receive: Receive = {
    case  clusterConfig: JsValue => dbOps.findClusters( Location (clusterConfig("longitude").as[String], clusterConfig("latitude").as[String]), clusterConfig("type").as[String] )
                                         .onComplete(out ! _.get )

    case _  => out ! "Not known"
  }}
