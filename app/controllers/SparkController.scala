package controllers

import javax.inject.{Inject, Singleton}

import org.apache.spark.{SparkConf, SparkContext}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents}
import services.postgres.DatabaseConnection
import org.apache.spark.mllib.classification.{NaiveBayes, NaiveBayesModel}
import org.apache.spark.mllib.util.MLUtils

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SparkController  @Inject()(cc: ControllerComponents)(postgres: DatabaseConnection)(implicit exec: ExecutionContext)   extends AbstractController(cc) {

  val conf = new SparkConf().setMaster("local[*]").setAppName(s"Spark App")

  val sc =  new SparkContext(conf)

  // Load and parse the data file.
  def estimatePi () = Action.async{
    Future {
      val count = sc.parallelize(1 to 1000000).filter{ _ =>
        val x = math.random
        val y = math.random
        x*x + y*y < 1
      }.count()

      Ok(s"Pi is roughly ${(4.0 * count)/1000000}")

    }
  }
}
