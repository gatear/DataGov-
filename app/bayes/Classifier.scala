package bayes

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import com.tsukaby.bayes.classifier.BayesClassifier
import play.api.Logger
import services.postgres.DatabaseConnection

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class Classifier @Inject() (postgres: DatabaseConnection, actorSystem: ActorSystem)(implicit exec: ExecutionContext) {

  val bayes = new BayesClassifier[String, String]()
  var available = false;

  //Learning from all descriptions
  //Use as many features as possible

  Future {
    postgres.messagesRepository.selectAll("Primary Type", "block")
      .map(_.get.foreach(seq => {
        bayes.learn(seq(0), seq(1)::Nil)
      }))
  }.onComplete( _ => {
    Logger.info("Naive Bayes classifier learning process finished ..."); available = true;
  })

  def predict(text: String) = {
    if (available) {
      Future {
        bayes.classify(text :: Nil).map(classification => (classification.category -> classification.probability))
      }
    } else {
      Future { Some("Not available yet ...") }
    }
  }
}