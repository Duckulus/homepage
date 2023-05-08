package us.duckul.homepage.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting() {
    routing {
        staticFiles("/", File("./static"))
    }
    install(IgnoreTrailingSlash) {
        routing {
            staticFiles("/", File("./static")) {
                extensions("html", "htm")
            }
        }
    }
    install(StatusPages) {
        statusFile(HttpStatusCode.NotFound, filePattern = "error#.html")
    }
}
