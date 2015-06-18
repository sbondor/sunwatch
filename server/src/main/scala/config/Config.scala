package config

import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

object Config {
  private val config = ConfigFactory.load()

  object geocoding {
    lazy val username = config.getString("geocoding.username")
  }
}