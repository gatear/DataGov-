package services.postgres

import com.github.mauricio.async.db.{Connection, QueryResult, ResultSet, RowData}
import play.api.libs.json._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AsyncRepo(pool :Connection) {
import AsyncRepo._

  def selectTotal_Arrests_Domestics() =  pool.sendPreparedStatement( total_arrests_domestics )

  def selectTypeSeries() = pool.sendPreparedStatement( selectCrimeTypeSeries )

  def listDistribution ():Future[IndexedSeq[Seq[String]]] = {

    val futureResult : Future[QueryResult] = pool.sendQuery(SelectCrimeDistribution)

    val result: Future[IndexedSeq[Seq[String]]] = futureResult.map((qResult: QueryResult) => qResult.rows
                                                                   .get     //todo let percentage be float and use tuples directly!!!
                                                                   .map {case row => Seq( row("type").asInstanceOf[String], row("percentage").asInstanceOf[BigDecimal].floatValue().toString ) })

    result
  }

  def selectAll(columns :String*): Future[Option[IndexedSeq[Seq[String]]]] = {
    pool.sendPreparedStatement(SelectAll).map( result => result.rows.map( resultSet => resultSet.map( row => columns.map( column => row(column) toString))))
  }
  import models.Location
  def checkLocation[L >: Location] (location: L) = {

    val futureResult  = pool.sendQuery(SelectCrimesNearLocation.replace("$1",location.toString))
    val result = futureResult.map( qResult => qResult.rows match { case Some(resultSet: ResultSet) => resultSet.collect{case row => (row("Primary Type").toString,
                                                                                                                                     row("Location Description").toString,
                                                                                                                                     row("description").toString,
                                                                                                                                     row("date").toString,
                                                                                                                                     row("id").toString) }})

    result
  }

  def selectCrimeDev () = {
    val futureResult : Future[QueryResult] = pool.sendQuery(SelectCrimeDev)
    val result =  pool.sendQuery(SelectCrimeDev).map( qResult => qResult.rows match { case Some(resultSet: ResultSet) => resultSet.collect{case row =>row("year").toString -> row("deviation").toString}})

    result
  }
  def selectDistribution(): Future[IndexedSeq[IndexedSeq[String]]] = {
    import services.postgres.AsyncRepo._

    val futureComputation = pool.sendQuery(SelectCrimeDistribution)

    futureComputation.collect{ case qResult => qResult.rows match { case Some(resultSet: ResultSet) => resultSet.collect{case r: RowData => r.map(_.toString)}}}
  }

 def findClusters ( location: Location, crimeType: String): Future[JsArray] = {
    pool.sendPreparedStatement( selectClusters.replace("$1",location.toString), Array(crimeType))
        .collect { case queryResult =>  Json.arr (queryResult.rows.get.collect{ case row =>
          Json.obj (
            "cluster" -> JsString( row("cluster_id") toString),
            "latitude" ->  JsString ( row("latitude") toString),
            "longitude" -> JsString ( row("longitude") toString)
          ) }) }
 }
}
object AsyncRepo {

  val selectCrimeTypeSeries = "SELECT  \"Primary Type\",year, COUNT(id) FROM crimes_2001_to_2017_chicago GROUP BY \"Primary Type\", year;"

  val total_arrests_domestics = "SELECT 'crimes' AS cluster, COUNT(arrest) AS number FROM crimes_2001_to_2017_chicago UNION SELECT 'arrests'  ,COUNT(arrest)  FROM crimes_2001_to_2017_chicago WHERE crimes_2001_to_2017_chicago.arrest = 'true' UNION SELECT 'domestics', COUNT(crimes_2001_to_2017_chicago.domestic) FROM crimes_2001_to_2017_chicago WHERE  crimes_2001_to_2017_chicago.domestic = 'true';"

  val selectClusters = "SELECT * FROM( SELECT id, date, longitude, latitude, st_clusterdbscan(geom, 0.0001, 200) OVER(ORDER BY date DESC) AS cluster_id FROM  (SELECT * FROM crimes_2001_to_2017_chicago WHERE \"Primary Type\"= ? AND st_dwithin( geom::geography, st_geomfromtext($1)::geography, 500)) AS proximityCrimes) AS clusters WHERE cluster_id IS NOT NULL;"

  val selectAllLocations = "SELECT  DISTINCT ON (label) CAST (date_part('year',date)::text || date_part('month',date )::text || date_part('day',date)::text AS BIGINT) AS label, latitude, longitude " +
                           "FROM crimes_2001_to_2017_chicago " +
                           "LIMIT 100 "

  val SelectCrimesNearLocation = "SELECT \"id\",\"Primary Type\",\"description\",\"date\"::DATE, \"Location Description\"\nFROM crimes_2001_to_2017_chicago\nWHERE ST_DWithin( geom::geography , st_geomfromtext($1)::geography , 100)  = TRUE LIMIT 40"

  val SelectCrimeDistribution =
    "(SELECT crimes.type, ROUND( ((CAST(crimes.cases AS NUMERIC) * 100)/ (SELECT COUNT(*) FROM crimes_2001_to_2017_chicago) ),4)  AS Percentage FROM " +
    "(SELECT DISTINCT  crimes_2001_to_2017_chicago.\"Primary Type\" AS type, COUNT(crimes_2001_to_2017_chicago.\"Primary Type\") AS Cases " +
    "FROM crimes_2001_to_2017_chicago " +
    "GROUP BY crimes_2001_to_2017_chicago.\"Primary Type\") AS  crimes) " +
    "ORDER BY Percentage DESC"
  val SelectAll = "SELECT * FROM  crimes_2001_to_2017_chicago LIMIT 100000;"

  val SelectCrimeDev = "SELECT year, crimes, ((crimes/(SELECT AVG(subQ.crimes) FROM (SELECT COUNT(id) crimes FROM crimes_2001_to_2017_chicago WHERE year <> 2001 GROUP BY year) subQ)::FLOAT)- 1)*100 deviation\nFROM (SELECT year, COUNT(id) crimes\n      FROM crimes_2001_to_2017_chicago\n      WHERE year <> 2001\n      GROUP BY year ) subQ2\nORDER BY year ASC"
}