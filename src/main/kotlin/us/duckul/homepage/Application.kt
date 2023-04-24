package us.duckul.homepage

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import us.duckul.homepage.plugins.configureChat
import us.duckul.homepage.plugins.configureRouting
import us.duckul.homepage.plugins.configureSerialization
import us.duckul.homepage.plugins.configureSockets

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSockets()
    configureSerialization()
    configureRouting()
    configureChat()
}
