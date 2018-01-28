package models

case class Location (long :String, lat: String) extends SpatialModel
{
  override def toString: String = "'POINT(" + long.toString + " " + lat.toString + ")'"
}

