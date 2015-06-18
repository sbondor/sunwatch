package api

import scala.xml.Node
import scala.xml.NodeSeq
import spray.http._
import spray.httpx.unmarshalling._
import org.joda.time.DateTime

object Joda {
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}

case class MeteoForecast(
  from: DateTime,
  to: DateTime,
  temperature: Option[Float],
  symbol: Option[Int]
)

object MeteoForecast {
  def fromXML(node: Node): MeteoForecast = {
    val temperatureNodes = node \\ "temperature"
    val temperature = if(temperatureNodes.length > 0) Some(temperatureNodes \@ "value" toFloat) else None
    val symbolNodes = node \\ "symbol"
    val symbol = if(symbolNodes.length > 0) Some(symbolNodes \@ "number" toInt) else None

    MeteoForecast(
      from = DateTime.parse(node \@ "from"),
      to = DateTime.parse(node \@ "to"),
      temperature = temperature,
      symbol = symbol
    )
  }
}

case class MeteoData(from: DateTime, to: DateTime, forecasts: Seq[MeteoForecast])

object MeteoData {
  import Joda._

  def fromXML(node: Node): MeteoData = {
    val models = node \ "meta" \ "model"

    MeteoData(
    // we extract the earliest 'from' of all models
    from = models map ( model => { DateTime.parse(model \@ "from")} ) min,
    // we extract the latter 'to' of all models
    to = models map ( model => { DateTime.parse(model \@ "to")} ) max,
    forecasts = ( node \ "product" \ "time") map MeteoForecast.fromXML)
  }
}

trait MeteoDataSupport {
  import spray.http.MediaTypes._

  implicit val MeteoDataUnmarshaller: Unmarshaller[MeteoData] = Unmarshaller.delegate[NodeSeq, MeteoData](`text/xml`, `application/xml`) {
    nodeSeq =>
      MeteoData.fromXML( nodeSeq.head )
    }
}