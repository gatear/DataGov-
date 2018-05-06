package models

import com.sun.org.glassfish.gmbal.Description
import play.api.libs.json.{JsValue, Json, Writes}

case class Crime(crimeType: String, locationDescription: String, date: String, id: Long , domestic:String = "False" ,arrest: String = "False", description: String= "")

object Crime {

  implicit val crimeWrites = new Writes[Crime] {
    override def writes(crime: Crime): JsValue = Json.obj (
      "Crime" -> crime.crimeType,
      "Description" -> crime.description,
      "Arrest" -> crime.arrest,
      "LocationDescription" -> crime.locationDescription,
      "Date" -> crime.date,
      "ID" -> crime.id
    )
    }
  }
