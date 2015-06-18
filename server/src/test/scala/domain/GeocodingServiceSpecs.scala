package domain

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.specs2.matcher.Scope
import org.specs2.mock._
import org.specs2.mutable.Specification
import scala.concurrent.Await
import scala.concurrent.duration._

class GeocodingServiceSpecs extends Specification with Mockito with LazyLogging {

  import Samples._

  val WAIT_DURATION = Duration(10,SECONDS)

  class TestScope extends TestKit(ActorSystem("test-actor-system", ConfigFactory.load())) with Scope with GeocodingComponentImpl {}

  "get" should {
    "return the GeoData for the given location" in new TestScope {
      val Right(geoData) = Await.result(geocodingService.get("Oslo"), WAIT_DURATION)
      geoData must beEqualTo(geoDataOslo)
    }

    "return an error if the location does not exist" in new TestScope {
      val Left(error) = Await.result(geocodingService.get("Hiperboreea"), WAIT_DURATION)
      error must beEqualTo("location_not_found")
    }
  }
}