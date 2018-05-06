package bayes

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import com.tsukaby.bayes.classifier.BayesClassifier
import play.api.Logger
import play.api.libs.json.{JsString, Json}
import services.postgres.{AnormRepo, DatabaseConnection}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Classifiers @Inject()(anorm: AnormRepo, actorSystem: ActorSystem)(implicit exec: ExecutionContext) {

  val arrestBayesClassifier = new BayesClassifier[String, String]()
  val domesticBayesClassifier = new BayesClassifier[String,String]()
  val safeBayesClassifier = new BayesClassifier[String,String]()

  //Learning from all descriptions
  //Use as many features as possible

  Future {
    anorm.getAll().map( crime => { arrestBayesClassifier.learn( crime.arrest, crime.crimeType::crime.locationDescription:: Nil ) })
  }.onComplete ( tryResult =>  Logger.warn(s"Naive Bayes Arrest Classifier Ready ...") )

  Future {
    anorm.getAll().map( crime => domesticBayesClassifier.learn( crime.domestic, crime.crimeType::crime.locationDescription::crime.arrest::Nil))
  }.onComplete( tryResult => Logger.warn(s"Naive Bayes Domestic Classifier Ready ..."))

  def predict(crime: String, location:String) = {
    Future {
      arrestBayesClassifier.classify(crime :: location :: Nil).map(classification => (classification.category))
    }.map(_.getOrElse("None")).flatMap(arrest =>
      Future {
        domesticBayesClassifier.classify(crime :: location :: arrest :: Nil).map(classification => Json.obj("arrest" -> JsString(arrest), "domestic" -> JsString(classification.category)))
      })
  }
}