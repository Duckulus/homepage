package us.duckul.homepage

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import us.duckul.homepage.plugins.*

fun main() {
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
