package domain

import api.MeteoData
import org.joda.time.{DateTimeZone, LocalDate, LocalTime, Period, Hours}
import scala.collection.mutable.HashMap

/** Trait transforming [[api.MeteoData]] to [[domain.WeatherData]]  */
trait WeatherService {

  private val ShortTermInterval = 3
  private val LongTermInterval = 6

  def toWeatherData(meteoData: MeteoData, dstOffset: Int): WeatherData = {
    // Maps having days as keys. The values are maps having hours as keys and tuple(temperature, symbol) as values.
    type ForecastMap = HashMap[LocalDate, HashMap[Int, Tuple2[Int, Int]]]
    val shortTerm: ForecastMap = HashMap.empty
    val longTerm: ForecastMap = HashMap.empty
    val zone = DateTimeZone.forOffsetHours(dstOffset)
    val fromMeteoLocal = meteoData.from.toDateTime(zone)
    val toMeteoLocal = meteoData.to.toDateTime(zone)

    meteoData.forecasts map ( forecast => {
      val from = forecast.from.toDateTime(zone)

      if(from.compareTo(fromMeteoLocal) >= 0 && from.compareTo(toMeteoLocal) < 0) {
        val fromHour = from.getHourOfDay
        val to = forecast.to.toDateTime(zone)
        val fromLocalDate = from.toLocalDate

        val periodFromStartDay = new Period(fromMeteoLocal.withTimeAtStartOfDay, from)
        val daysFromStart = periodFromStartDay.getWeeks * 7 + periodFromStartDay.getDays

        val periodFromStart = new Period(fromMeteoLocal, from)
        val hoursFromStart = periodFromStart.getDays * 24 + periodFromStart.getHours

        if (from == to && forecast.temperature.isDefined) {
          // extract temperature

          // Check that the hour matches the short term interval
          if (daysFromStart < 3 && (hoursFromStart % ShortTermInterval == 0 )) {
            // short term
            val shortTermDay = shortTerm.getOrElseUpdate(fromLocalDate, HashMap.empty)
            val hourForecast = shortTermDay.getOrElseUpdate(fromHour, (-1, -1))
            shortTermDay(fromHour) = hourForecast.copy(_1 = forecast.temperature.get.round)
          }

          // For long term forecast, we are only interested in the hours (0, 6, 12, 18)
          if (daysFromStart >= 2 && (forecast.from.getHourOfDay % LongTermInterval == 0)) {
            // long term
            val longTermDay = longTerm.getOrElseUpdate(fromLocalDate, HashMap.empty)
            val hourForecast = longTermDay.getOrElseUpdate(fromHour, (-1, -1))
            longTermDay(fromHour) = hourForecast.copy(_1 = forecast.temperature.get.round)
          }
        }
        else if (forecast.symbol.isDefined) {
          // extract symbol

          val hoursBetween = Hours.hoursBetween(from, to).getHours
          if (daysFromStart < 3 && (hoursFromStart % ShortTermInterval == 0) && hoursBetween == ShortTermInterval) {
            // short term
            val shortTermDay = shortTerm.getOrElseUpdate(fromLocalDate, HashMap.empty)
            val hourForecast = shortTermDay.getOrElseUpdate(fromHour, (-1, -1))
            shortTermDay(fromHour) = hourForecast.copy(_2 = forecast.symbol.get)
          }

          // For long term forecast, we are only interested in the hours (0, 6, 12, 18)
          if (daysFromStart >= 2 && (forecast.from.getHourOfDay % LongTermInterval) == 0 && hoursBetween == LongTermInterval) {
            // long term
            val longTermDay = longTerm.getOrElseUpdate(fromLocalDate, HashMap.empty)
            val hourForecast = longTermDay.getOrElseUpdate(fromHour, (-1, -1))
            longTermDay(fromHour) = hourForecast.copy(_2 = forecast.symbol.get)
          }
        }
      }
    })

    def sort(forecastMap: ForecastMap) = forecastMap.toList
      .sortWith( { case((hourLeft, _), (hourRight, _)) => hourLeft.compareTo(hourRight) < 0 } )
      .map({ case (day, hours) => DailyData(
      day = day.toString("dd/MM/yyyy"),
      data = hours.toList
        .sortBy(_._1)
        .map( { case(hour, (temp, symbol)) => HourData(
        startTime = new LocalTime(hour, 0).toString("HH:mm"),
        temperature = temp,
        symbol = symbol)})
    )})

    WeatherData(shortterm = sort(shortTerm), longterm = sort(longTerm))
  }
}
