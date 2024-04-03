package us.duckul.homepage.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

const val MAX_MESSAGES = 250

@Serializable
data class Message(val sender: String, val text: String, val users: Int)

@Serializable
data class IncomingMessage(val username: String, val message: String)

class Connection(val session: DefaultWebSocketSession) {
  companion object {
    val lastId = AtomicInteger(0)
  }

  val name = "anon_${lastId.getAndIncrement()}"
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

    suspend fun systemMessage(message: Message) {
      connections.forEach {
        it.session.send(Json.encodeToString(message))
      }
    }

    webSocket("/ws") {
      println(connections.size)
      val connection = Connection(this)
      try {
        connections += connection
        connection.session.send(Json.encodeToString(messages.toList()))
        systemMessage(Message("System", "", connections.size))
        for (frame in incoming) {
          if (frame is Frame.Text) {
            val text = frame.readText()
            val message = Json.decodeFromString<IncomingMessage>(text)
            val messageText = message.message
            val sender = message.username.ifBlank { connection.name }
            if (messageText.isNotBlank() && messageText.length <= 1000) {
              broadcast(Message(sender, messageText, connections.size))
            }
          }
        }
      } catch (e: Exception) {
        println(e.message)
      } finally {
        connections -= connection
        systemMessage(Message("System", "", connections.size))
      }

    }
  }
}
