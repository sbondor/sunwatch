package domain

import akka.actor.{Actor, ActorSystem, ActorRef}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import api.JsonFormats

import spray.can.Http
import spray.http._
import HttpMethods._
import spray.httpx.Json4sSupport
import spray.httpx.unmarshalling._

import config.Config

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/** Represents geocoding data for a specific location.
  *
  * @param latitude The latitude of the location.
  * @param longitude The longitude of the location.
  * @param dstOffset Daylight Saving Time (DST) offset of the location.
  */
case class GeoData(
  latitude: Float,
  longitude: Float,
  dstOffset: Int
)

/** Companion object providing helpful constants. */
object GeocodingComponent {
  val LocationNotFound = "location_not_found"
  val InternalServerError = "internal_server_error"
}

/** The interface of a component providing a GeocodingService.
  *
  * It consists of a single method which returns the wrapped GeocodingService.
  */
trait GeocodingComponent {

  def geocodingService: GeocodingService

  /** A interface providing geocoding services. */
  trait GeocodingService {

    /** Tries to retrieve geocoding data based on the provided location.
      *
      * @param location The name of the location.
      * @return A future containing geocoding data inside a [[domain.GeoData]] object or an error if there was a problem.
      */
    def get(location: String): Future[Either[String, GeoData]]
  }
}

private case class GeonamesTimezone(
  dstOffset: Int
)

private case class GeonamesLocation(
  lng: String,
  lat: String,
  timezone: GeonamesTimezone
)

private case class GeonamesSearch(
  geonames: List[GeonamesLocation]
)


/** A component providing an implementation of the [[domain.GeocodingComponent.GeocodingService]].
  *
  * It requires an implicit ActorSystem object used by the [[domain.GeocodingComponent.GeocodingService]] implementation.
  */
trait GeocodingComponentImpl extends GeocodingComponent {

  val geocodingService = new GeocodingServiceImpl

  protected[this] implicit def system: ActorSystem

  /** Implementation of the GeocodingService interface using the external 'http://api.met.no' webservice.
    *
    * It creates a new http connector on the actor system and transforms the data obtained from the webservice.
    */
  class GeocodingServiceImpl extends GeocodingService with Json4sSupport with JsonFormats {
    import GeocodingComponent._


    private implicit val timeout: Timeout = 15.seconds

    val host = "api.geonames.org"
    def connector: Future[ActorRef] = {
      IO(Http) ? Http.HostConnectorSetup(host, port = 80) map {
        case Http.HostConnectorInfo(hostConnector, _) => hostConnector
      }
    }

    def get(location: String): Future[Either[String, GeoData]] = {
      val uri = Uri("/searchJSON") withQuery ("formatted" -> "true", "q" -> location, "maxRows" -> "1",
        "lang" -> "es", "username" -> Config.geocoding.username, "style" -> "full")
      for {
        hostConnector <- connector
        response <- hostConnector.ask(HttpRequest(GET, uri)).mapTo[HttpResponse]
      } yield response.status match {
        case StatusCodes.OK => response.entity.as[GeonamesSearch] match {
          case Right(result) if result.geonames.size > 0 => {
            // We assume that the first entry is the best match
            val geodata = result.geonames(0)
            Right(GeoData(geodata.lat.toFloat, geodata.lng.toFloat, geodata.timezone.dstOffset))
          }
          case Right(result) => Left(LocationNotFound)
        }
        case _ => Left(InternalServerError)
      }
    }
  }
}