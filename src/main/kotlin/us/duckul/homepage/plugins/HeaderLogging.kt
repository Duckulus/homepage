package us.duckul.homepage.plugins

import io.ktor.server.application.*
import io.ktor.util.*

val HeaderLoggingPlugin = createApplicationPlugin(name = "HeaderLogging") {
    onCall { call ->
        println("Headers: ${call.request.headers.toMap().entries.joinToString()}")
    }
}