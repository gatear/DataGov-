package services.postgres

import com.github.mauricio.async.db.{Connection, QueryResult, ResultSet, RowData}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global




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
    val result = futureResult.map( qResult => qResult.rows match { case Some(resultSet: ResultSet) => resultSet.collect{case row =>row("Primary Type").toString -> row("description").toString}})

    result
  }

  def selectCrimeDev () = {
    val futureResult : Future[QueryResult] = pool.sendQuery(SelectCrimeDev)
    val result = futureResult.map( qResult => qResult.rows match { case Some(resultSet: ResultSet) => resultSet.collect{case row =>row("year").toString -> row("deviation").toString}})

    result
  }
  def selectDistribution(): Future[IndexedSeq[IndexedSeq[String]]] = {
    import services.postgres.MessageRepository._

    val futureComputation = pool.sendQuery(SelectCrimeDistribution)

    futureComputation.collect{ case qResult => qResult.rows match { case Some(resultSet: ResultSet) => resultSet.collect{case r: RowData => r.map(_.toString)}}}
  }
}
object MessageRepository {
  val SelectCrimesNearLocation = "SELECT \"Primary Type\",\"description\"\nFROM crimes_2001_to_2017_chicago\nWHERE ST_DWithin( geom::geography , st_geomfromtext($1)::geography , 50)  = TRUE ;"

  val SelectCrimeDistribution = "(SELECT crimes.type, ROUND( ((CAST(crimes.cases AS NUMERIC) * 100)/ (SELECT COUNT(*) FROM crimes_2001_to_2017_chicago) ),4)  AS Percentage FROM " +
    "(SELECT DISTINCT  crimes_2001_to_2017_chicago.\"Primary Type\" AS type, COUNT(crimes_2001_to_2017_chicago.\"Primary Type\") AS Cases " +
    "FROM crimes_2001_to_2017_chicago " +
    "GROUP BY crimes_2001_to_2017_chicago.\"Primary Type\") AS  crimes) " +
    "ORDER BY Percentage DESC"

  val SelectCrimeDev = "SELECT year, crimes, ((crimes/(SELECT AVG(subQ.crimes) FROM (SELECT COUNT(id) crimes FROM crimes_2001_to_2017_chicago WHERE year <> 2001 GROUP BY year) subQ)::FLOAT)- 1)*100 deviation\nFROM (SELECT year, COUNT(id) crimes\n      FROM crimes_2001_to_2017_chicago\n      WHERE year <> 2001\n      GROUP BY year ) subQ2\nORDER BY year ASC"
}