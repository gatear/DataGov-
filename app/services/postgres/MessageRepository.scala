package services.postgres

import com.github.mauricio.async.db.{Connection, QueryResult, ResultSet, RowData}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag



class MessageRepository(pool :Connection) {

  case class Row(data :String*)

  case class Model (data :String, data2: String){

  }

  def listDistribution ():Future[IndexedSeq[Seq[String]]] = {


    val futureResult : Future[QueryResult] = pool.sendQuery(MessageRepository.SelectCrimeDistribution)

    val result: Future[IndexedSeq[Seq[String]]] = futureResult.map((qResult: QueryResult) => qResult.rows
                                                                   .get
                                                                   .map {case row => Seq( row("type").asInstanceOf[String], row("percentage").asInstanceOf[BigDecimal].floatValue().toString ) })

    result
  }



  def selectDistribution(): Future[IndexedSeq[IndexedSeq[String]]] = {
    import services.postgres.MessageRepository._

    val futureComputation = pool.sendQuery(SelectCrimeDistribution)

    futureComputation.collect{ case qResult => qResult.rows match { case Some(resultSet: ResultSet) => resultSet.collect{case r: RowData => r.map(_.toString)}}}
  }
}
object MessageRepository {

  val SelectCrimeDistribution = "(SELECT crimes.type, ROUND( ((CAST(crimes.cases AS NUMERIC) * 100)/ (SELECT COUNT(*) FROM crimes_2001_to_2016) ),4)  AS Percentage FROM " +
    "(SELECT DISTINCT  crimes_2001_to_2016.\"Primary Type\" AS type, COUNT(crimes_2001_to_2016.\"Primary Type\") AS Cases " +
    "FROM crimes_2001_to_2016 " +
    "GROUP BY crimes_2001_to_2016.\"Primary Type\") AS  crimes) " +
    "ORDER BY Percentage DESC"
}