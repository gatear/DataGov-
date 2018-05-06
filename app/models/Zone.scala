package models

import play.api.libs.json.{JsValue, Json, Writes}

case class Zone (cluster: Long, status: String)

object Zone {
  implicit val zoneWrites = new Writes[Zone] {
    override def writes(zone: Zone):JsValue= Json.obj(
      "cluster" -> zone.cluster,
      "status" -> zone.status
    )
  }
}
