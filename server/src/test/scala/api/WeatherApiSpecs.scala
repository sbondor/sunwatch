package api

import akka.testkit.TestActorRef
import domain._
import org.specs2.matcher.Scope
import org.specs2.mock._
import org.specs2.mutable.Specification
import spray.http.StatusCodes
import spray.httpx.Json4sSupport
import spray.testkit.Specs2RouteTest
import scala.concurrent.duration.FiniteDuration

import akka.actor._

class WeatherApiSpecs extends Specification with Specs2RouteTest with Mockito with Json4sSupport with JsonFormats {

  implicit val routeTestTimeout = RouteTestTimeout(FiniteDuration(5, "second"))

  class MockedScope(val forecastRef: ActorRef) extends Scope with WeatherApi {

    def actorRefFactory = system
  }

  val sampleWeatherData = WeatherData(
    shortterm = List(DailyData(
      day = "2015-06-17",
      data = List(HourData(
        startTime = "02:00",
        temperature = 21,
        symbol = 1
      ))
    )),
    longterm = List(

    )
  )

  val location = "Oslo"
  val sampleForecast = Forecast(location = location)

  "GET /api/weather" should {

    val successForecastRef = TestActorRef(new Actor {
      def receive = {
        case sampleForecast => sender() ! Right(sampleWeatherData)
      }
    })

    "returns the weather data for the given location" in new MockedScope(successForecastRef) {
      Get(s"/api/weather?location=$location") ~> weatherRoute ~> check {
        status === StatusCodes.OK
        responseAs[WeatherData] mustEqual(sampleWeatherData)
      }
    }

    val failForecastRef = TestActorRef(new Actor {
      def receive = {
        case Forecast(_) => sender() ! Left(ForecastActor.InternalServerError)
      }
    })

    "returns 404 if there are errors coming from the forecasting actor" in new MockedScope(failForecastRef) {
      Get(s"/api/weather?location=$location") ~> weatherRoute ~> check {
        status === StatusCodes.NotFound
        responseAs[ErrorWrapper] mustEqual (ErrorWrapper(ForecastActor.InternalServerError))
      }
    }
  }
}