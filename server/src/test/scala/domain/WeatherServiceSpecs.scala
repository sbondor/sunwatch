package domain

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.specs2.matcher.Scope
import org.specs2.mock._
import org.specs2.mutable.Specification

class WeatherServiceSpecs extends Specification with Mockito with LazyLogging {
  import Samples._

  trait TestScope extends Scope with WeatherService {}

  "toWeatherData" should {
    "extract and return the relevant weather data" in new TestScope {
      val weatherData = toWeatherData(meteoDataOslo, dstOffsetOslo)

      weatherData.shortterm must have size(3)
      val firstDay = weatherData.shortterm(0)
      firstDay.day must beEqualTo("18/06/2015")
      firstDay.data must have size(5)
      val firstHour = firstDay.data(0)
      firstHour.startTime must beEqualTo("09:00")
      firstHour.temperature must beEqualTo(12)
      firstHour.symbol must beEqualTo(4)

      weatherData.longterm must have size(8)
      val longTermDay = weatherData.longterm(0)
      longTermDay.day must beEqualTo("20/06/2015")
      longTermDay.data must have size(4)
      val longTermHour = longTermDay.data(0)
      longTermHour.startTime must beEqualTo("02:00")
      longTermHour.temperature must beEqualTo(11)
      longTermHour.symbol must beEqualTo(1)
    }
  }
}