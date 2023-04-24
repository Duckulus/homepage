package us.duckul.homepage.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

const val MAX_MESSAGES = 250

@Serializable
data class Message(val sender: String, val text: String, val users: Int)

class Connection(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }

    val name = "user_${lastId.getAndIncrement()}"
}

fun Application.configureChat() {
    routing {
        val mutex = Mutex()
        val messages = ArrayBlockingQueue<Message>(MAX_MESSAGES)
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())

        suspend fun broadcast(message: Message) {
            connections.forEach {
                it.session.send(Json.encodeToString(message))
            }
            mutex.withLock {
                if (!messages.offer(message)) {
                    messages.poll()
                    messages.offer(message)
                }
            }
        }

        webSocket("/ws") {
            println(connections.size)
            val connection = Connection(this)
            try {
                connections += connection
                connection.session.send(Json.encodeToString(messages.toList()))
                broadcast(Message("System", "User ${connection.name} joined the chat", connections.size))
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text.length <= 1000) {
                            val message = Message(connection.name, text, connections.size)
                            broadcast(message)
                        }
                    }
                }
            } catch (e: Exception) {
                println(e.localizedMessage)
            } finally {
                connections -= connection
                broadcast(Message("System", "User ${connection.name} left the chat", connections.size))
            }

        }
    }
}