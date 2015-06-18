package domain

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.specs2.matcher.Scope
import org.specs2.mock._
import org.specs2.mutable.{After, Specification}
import org.specs2.time.NoTimeConversions

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

class ForecastActorSpecs extends Specification with NoTimeConversions with Mockito with LazyLogging {

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  class ActorTest extends TestKit(ActorSystem("test-actor-system", ConfigFactory.load())) with ImplicitSender with Scope with After {
    def after { TestKit.shutdownActorSystem(system) }

  }

  trait MockGeocodingComponent extends GeocodingComponent {
    val geocodingService = mock[GeocodingService]
  }

  "Forecast messages" should {

    trait SuccessfulGeocodingComponent extends MockGeocodingComponent {
      geocodingService.get("Oslo") returns Future { Right(GeoData(latitude = 46.18333f, longitude = 21.31667f, dstOffset = 1)) }
    }

    "reply with forecast data for the specified location" in new ActorTest {
      val forecastActor = system.actorOf(Props(new ForecastActor(system) with SuccessfulGeocodingComponent))

      forecastActor ! Forecast(location = "Oslo")
      receiveOne(5 seconds).asInstanceOf[Either[String, WeatherData]] must beRight
    }

    val mythicalLocation = "Hiperboreea"
    trait FailingGeocodingComponent extends MockGeocodingComponent {
      geocodingService.get(mythicalLocation) returns Future { Left(GeocodingComponent.LocationNotFound) }
    }

    "reply with error if the location is not found" in new ActorTest {
      val forecastActor = system.actorOf(Props(new ForecastActor(system) with FailingGeocodingComponent))

      forecastActor ! Forecast(location = mythicalLocation)
      receiveOne(5 seconds).asInstanceOf[Either[String, WeatherData]] must beLeft(GeocodingComponent.LocationNotFound)
    }
  }
}