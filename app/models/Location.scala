package models

case class Location (long :String, lat: String)
{
  override def toString: String = "'POINT(" + long.toString + " " + lat.toString + ")'"
}
