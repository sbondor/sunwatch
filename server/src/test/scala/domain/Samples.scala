package domain

import api.MeteoData
import scala.xml.XML

object Samples {
  private lazy val sampleMeteoXML = XML.load(getClass.getClassLoader.getResourceAsStream("sampleOsloMeteo.xml"))
  lazy val meteoDataOslo = MeteoData.fromXML(sampleMeteoXML)
  lazy val dstOffsetOslo = 2

  lazy val geoDataOslo = GeoData(
    latitude = 59.91273f,
    longitude = 10.74609f,
    dstOffset = 2
  )
}