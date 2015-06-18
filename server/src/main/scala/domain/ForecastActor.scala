package domain

import api.{MeteoData, MeteoDataSupport}

import akka.actor.{ActorRef, Actor, Props, ActorSystem}
import akka.io.IO
import akka.util.Timeout
import akka.pattern.ask

import com.typesafe.scalalogging.slf4j.LazyLogging
import spray.can.Http
import spray.http.HttpMethods._
import spray.http.{StatusCodes, HttpResponse, HttpRequest}
import spray.httpx.unmarshalling._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** A message for the [[domain.ForecastActor]].
  *
  * It tells the [[domain.ForecastActor]] to return the [[domain.WeatherData]] for the given location.
  */
case class Forecast(location: String)


/** Represents the forecast for a specific hour.
  *
  * @param startTime The hour of the forecast.
  * @param temperature The forecasted temperature.
  * @param symbol The symbol representing the type of weather forecasted ( see http://http://api.met.no/weatherapi/weathericon/1.1/documentation ).
  */
case class HourData(
  startTime: String,
  temperature: Int,
  symbol: Int
)


/** Represents the forecast for a specific day.
  *
  * @param day The day of the forecast in 'dd/MM/yyyy' format.
  * @param data A list of [[domain.HourData]].
  */
case class DailyData(
  day: String,
  data: List[HourData]
)


/** Represents the forecast for a specific location.
  *
  * @param shortterm Short term forecast containing [[domain.DailyData]] for the first 3 days.
  * @param longterm Long term forecast containing [[domain.DailyData]] starting with the third day until the ninth day.
  */
case class WeatherData(
  shortterm: List[DailyData],
  longterm: List[DailyData]
)

/** Factory for [[akka.actor.Props]] containing [[domain.ForecastActor]] instances. */
object ForecastActor {

  val InternalServerError = "internal_server_error"
  val NoMeteoData = "no_data_for_location"

  /**
   * Create Props for an actor of this type.
   * @param actorSystem The actor system which the new actor will belong to.
   * @return a Props for creating this actor.
   */
  def props(actorSystem: ActorSystem): Props = Props(new ForecastActor(actorSystem) with GeocodingComponentImpl)

}


/** An actor generating [[domain.WeatherData]].
  *
  * It integrates geocoding information from the [[domain.GeocodingComponent.GeocodingService]] with weather forecasts from 'http://api.met.no'.
  */
abstract class ForecastActor(val actorSystem: ActorSystem) extends Actor
  with WeatherService
  with MeteoDataSupport
  with GeocodingComponent
  with LazyLogging {

  import ForecastActor._

  private implicit val timeout: Timeout = 5.seconds
  protected[this] implicit def system: ActorSystem = context.system

  val host = "api.met.no"
  def connector: Future[ActorRef] = {
    IO(Http) ? Http.HostConnectorSetup(host, port = 80) map {
      case Http.HostConnectorInfo(hostConnector, _) => hostConnector
    }
  }

  def receive = {
    case Forecast(location) =>
      val senderRef = sender()

      try {
        for {
          geocodingResult <- geocodingService.get(location)
        } yield geocodingResult match {
          case Right(geodata) => {
            val uri = s"/weatherapi/locationforecast/1.9/?lat=${geodata.latitude}&lon=${geodata.longitude}"
            println(uri)

            for {
              hostConnector <- connector
              response <- (hostConnector ? HttpRequest(GET, uri)).mapTo[HttpResponse]
            } yield response.status match {
              case StatusCodes.OK => response.entity.as[MeteoData] match {
                case Right(meteoData) => senderRef ! Right(toWeatherData(meteoData, geodata.dstOffset))
                case Left(error) => {
                  logger.error(error.toString)
                  senderRef ! Left(InternalServerError)
                }
              }
              case StatusCodes.BadRequest => senderRef ! Left(NoMeteoData)
              case _ => senderRef ! Left(InternalServerError)
            }
          }
          case Left(error) => senderRef ! Left(error)
        }
      }
      catch {
        case ex => {
          logger.debug(ex.getMessage)
          senderRef ! Left(InternalServerError)
        }
      }
  }
}

