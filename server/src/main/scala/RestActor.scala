import akka.actor.{ActorRef, Actor}
import api.WeatherApi

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class RestActor(val forecastRef: ActorRef) extends Actor
  with WeatherApi {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  implicit def executionContext = actorRefFactory.dispatcher

  def receive = runRoute(weatherRoute)
}