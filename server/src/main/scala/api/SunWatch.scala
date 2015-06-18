package api

import com.typesafe.scalalogging.slf4j.LazyLogging
import spray.httpx.Json4sSupport
import spray.routing.HttpService

/** Wrapper for an error string. Used by the apis to return errors in the JSON payload.
  *
  * @param error The error string being wrapped.
  */
case class ErrorWrapper(
  error: String
)

trait SunWatchApi extends HttpService with Json4sSupport with JsonFormats with LazyLogging