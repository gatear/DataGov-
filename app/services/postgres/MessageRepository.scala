package services.postgres

import com.github.mauricio.async.db.{Connection, QueryResult, ResultSet, RowData}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag



class MessageRepository(pool :Connection) {
import MessageRepository._



  def listDistribution ():Future[IndexedSeq[Seq[String]]] = {


    val futureResult : Future[QueryResult] = pool.sendQuery(SelectCrimeDistribution)

    val result: Future[IndexedSeq[Seq[String]]] = futureResult.map((qResult: QueryResult) => qResult.rows
                                                                   .get     //todo let percentage be float and use tuples directly!!!
                                                                   .map {case row => Seq( row("type").asInstanceOf[String], row("percentage").asInstanceOf[BigDecimal].floatValue().toString ) })

    result
  }

  import models.Location
  def checkLocation (location: Location): Future[IndexedSeq[(String, String)]] ={


    val futureResult : Future[QueryResult] = pool.sendQuery(SelectCrimesNearLocation.replace("$1",location.toString))
    val result = futureResult.map( qResult => qResult.rows match { case Some(resultSet: ResultSet) => resultSet.collect{case row =>row("Primary Type").toString -> row("distance").toString}})

    result
  }


  def selectDistribution(): Future[IndexedSeq[IndexedSeq[String]]] = {
    import services.postgres.MessageRepository._

    val futureComputation = pool.sendQuery(SelectCrimeDistribution)

    futureComputation.collect{ case qResult => qResult.rows match { case Some(resultSet: ResultSet) => resultSet.collect{case r: RowData => r.map(_.toString)}}}
  }
}
object MessageRepository {
  val SelectCrimesNearLocation = "SELECT \"Primary Type\", MIN(st_distance_sphere( st_centroid(geom), st_geomfromtext($1) )) distance\n  FROM crimes_2001_to_2017_chicago\n WHERE year > 2015\n GROUP BY \"Primary Type\"\nORDER BY distance"

  val SelectCrimeDistribution = "(SELECT crimes.type, ROUND( ((CAST(crimes.cases AS NUMERIC) * 100)/ (SELECT COUNT(*) FROM crimes_2001_to_2017_chicago) ),4)  AS Percentage FROM " +
    "(SELECT DISTINCT  crimes_2001_to_2017_chicago.\"Primary Type\" AS type, COUNT(crimes_2001_to_2017_chicago.\"Primary Type\") AS Cases " +
    "FROM crimes_2001_to_2017_chicago " +
    "GROUP BY crimes_2001_to_2017_chicago.\"Primary Type\") AS  crimes) " +
    "ORDER BY Percentage DESC"
}