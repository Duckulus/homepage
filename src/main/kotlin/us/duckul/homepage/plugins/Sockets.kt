package us.duckul.homepage.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

@Serializable
data class Message(val sender: String, val text: String, val users: Int)

class Connection(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }

    val name = "user_${lastId.getAndIncrement()}"
}

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val messages = LinkedBlockingQueue<Message>(100)
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())

        suspend fun broadcast(message: Message) {
            connections.forEach {
                it.session.send(Json.encodeToString(message))
            }
            messages.put(message)
        }

        webSocket("/chat") {
            connections.clear()
            println(connections.size)
            val connection = Connection(this)
            try {
                connections += connection
                connection.session.send(Json.encodeToString(messages.toList()))
                broadcast(Message("System", "User ${connection.name} joined the chat", connections.size))
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val message = Message(connection.name, text, connections.size)
                        broadcast(message)
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                connections -= connection
            }

        }
    }
}
