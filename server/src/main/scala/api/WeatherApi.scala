package api

import akka.util.Timeout
import domain.{Forecast, WeatherData}
import spray.http.StatusCodes
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Right, Failure, Success}
import akka.actor.{ActorRef, Actor}
import akka.pattern.ask

trait WeatherApi extends SunWatchApi {

  private implicit val timeout: Timeout = 15.seconds

  val forecastRef: ActorRef

  def weatherRoute(implicit ec: ExecutionContext) = pathPrefix("api" / "weather") {
    pathEndOrSingleSlash {
      get {
        parameter('location) { location =>
          logger.debug(s"GET api/weather?location=${location}")
          onComplete((forecastRef ? Forecast(location)).mapTo[Either[String, WeatherData]] ) {
            case Success(Right(data)) => complete(data)
            case Success(Left(error)) => complete(StatusCodes.NotFound, ErrorWrapper(error))
            case Failure(failure) => failure.printStackTrace();complete(StatusCodes.BadRequest)
          }
        }
      }
    }
  }
}
