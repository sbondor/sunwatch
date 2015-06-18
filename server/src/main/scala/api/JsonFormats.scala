package api

import org.json4s._
import org.json4s.ext._
import org.json4s.prefs.EmptyValueStrategy
import org.json4s.native.Serialization
import org.joda.time.DateTime

import domain._

trait JsonFormats {

  implicit val json4sFormats =
    new DefaultFormats {
      override val emptyValueStrategy = EmptyValueStrategy.preserve
    } ++ org.json4s.ext.JodaTimeSerializers.all
}