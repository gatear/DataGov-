package services.postgres

import javax.inject.Inject

import anorm._
import models._
import play.api.db._

class AnormRepo @Inject()(database: Database) {

   //Row Parsers
   val crimeParser : RowParser[Crime] = Macro.parser[Crime]("type", "location", "date", "id",  "domestic","arrest","description")
   val clusterParser : RowParser[Cluster] = Macro.parser[Cluster]("id","date","longitude","latitude","cluster_id")
   val ratingParser : RowParser[Rating] = Macro.parser[Rating]("value","ward","percentage")
   val zoneParser: RowParser[Zone] = Macro.parser[Zone]("cluster","status")

   def getAll (): List[Crime] = {
     database.withConnection { implicit conn =>
       SQL( """
            SELECT DISTINCT ON (description) "Primary Type" AS type , arrest, domestic, "Location Description" AS location, to_char(  crimes_2001_to_2017_chicago.date, 'MM-DD-YYYY HH24:MI:SS') AS date, id, description
            FROM crimes_2001_to_2017_chicago;
            """).as( crimeParser.*)
     }
   }

  /**To Show on map*/
   def clusterScan (location :Location, distance: Long, crimeType: String, events: Long) = {
     database.withConnection { implicit conn =>
       SQL(
         s"""
           SELECT * FROM( SELECT id,  to_char( date, 'MM-DD-YYYY HH24:MI') AS date, longitude::text , latitude::text, st_clusterdbscan(geom, 0.0001, $events) OVER(ORDER BY date DESC) AS cluster_id
                          FROM  (SELECT *
                                 FROM crimes_2001_to_2017_chicago
                                 WHERE "Primary Type"= '$crimeType' AND st_dwithin( geom::geography, st_geomfromtext( $location )::geography, $distance)) AS proximityCrimes) AS clusters
           WHERE cluster_id IS NOT NULL;
         """).as( clusterParser.* )
     }
   }

  /**To scan safe zones*/
  def classifyZone (location: Location, distance: Long, crimeType: String, events: Long) = {
    database.withConnection{ implicit conn =>
      SQL(
        s"""
          SELECT cluster_id AS cluster, CASE WHEN (count(id) > 1000) THEN 'DANGEROUS' ELSE 'SAFE' END status
          FROM( SELECT id, date, longitude, latitude, st_clusterdbscan(geom, eps := 0.1, minpoints := $events) OVER(ORDER BY date DESC) AS cluster_id
                 FROM  (SELECT *
                        FROM crimes_2001_to_2017_chicago
                        WHERE "Primary Type"= '$crimeType' AND st_dwithin( geom::geography, st_geomfromtext($location)::geography, $distance)) AS proximityCrimes) AS clusters
           WHERE cluster_id IS NOT NULL
           GROUP BY cluster
         """).as( zoneParser.* )
    }
  }


  def getTrainingSet () = {
    database.withConnection{ implicit  conn =>
      SQL(
        """
           SELECT crimes_2001_to_2017_chicago.ward AS ward, rating::text AS value,  COUNT(id)::FLOAT /count::FLOAT * 100 AS percentage
           FROM crimes_2001_to_2017_chicago
           INNER JOIN ratings ON crimes_2001_to_2017_chicago.ward = ratings.ward
           WHERE "Primary Type" = 'THEFT'
           GROUP BY crimes_2001_to_2017_chicago.ward, ratings.rating, ratings.count;
        """).as( ratingParser.* )
    }
  }


}
