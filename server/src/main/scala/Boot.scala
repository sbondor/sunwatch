import akka.actor.{ActorSystem, Props}
import akka.io.IO
import domain.{GeocodingComponentImpl, ForecastActor}
import spray.can.Http

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("sunwatch")

  // create the forecast actor
  val forecastActor = system.actorOf(ForecastActor.props(system), "forecast-actor")

  // create and start our service actor
  val restActor = system.actorOf(Props(classOf[RestActor], forecastActor), "rest-actor")

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ! Http.Bind(restActor, "localhost", port = 8080)
}