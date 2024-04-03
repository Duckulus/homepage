package us.duckul.homepage

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import us.duckul.homepage.plugins.*
import java.io.File
import java.nio.file.Files

fun saveResource(name: String, output: String) {
  val file = File("./$output")
  if (!file.exists()) {
    val stream = Application::class.java.getResourceAsStream("/$name")
    Files.copy(stream!!, file.toPath())
  }
}

fun main() {
  File("./garfield").mkdirs()
  saveResource("garfield.mp4", "garfield/garfield.mp4")
  saveResource("gilligan.ttf", "garfield/gilligan.ttf")

  embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
    .start(wait = true)
}

fun Application.module() {
  install(HeaderLoggingPlugin)
  configureSockets()
  configureSerialization()
  configureRouting()
  configureChat()
}
