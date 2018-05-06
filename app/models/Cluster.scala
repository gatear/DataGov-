package models

import play.api.libs.json.{JsValue, Json, Writes}

case class Cluster(id: Long, date: String, long: String, lat: String, cluster_id: Long )

object Cluster {

  implicit val clusterWrites = new Writes[Cluster] {
    override def writes(cluster: Cluster): JsValue = Json.obj (
      "ID" -> cluster.id,
      "Date" -> cluster.date,
      "Cluster_ID" -> cluster.cluster_id,
      "Latitude" -> cluster.lat,
      "Longtitude" -> cluster.long
    )
  }

}
