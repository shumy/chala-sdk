package net.chala.server

import io.javalin.Javalin
import io.javalin.websocket.WsConnectContext
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

private val LOGGER = LoggerFactory.getLogger(WsChannel::class.java)

internal class WsChannel(server: Javalin, private val path: String) {
  private val clients = ConcurrentHashMap<String, WsConnectContext>()

  init {
    server.ws(path) { wsc ->
      wsc.onConnect {
        LOGGER.info("WS open (path=$path, session=${it.sessionId})")
        clients[it.sessionId] = it
      }

      wsc.onClose {
        LOGGER.info("WS close (path=$path, session=${it.sessionId})")
        clients.remove(it.sessionId)
      }

      wsc.onError {
        LOGGER.error("WS error (path=$path, session=${it.sessionId})")
      }
    }
    LOGGER.info("CHANNEL {}", path)
  }

  fun publish(msg: String) = clients.values.forEach {
    LOGGER.info("WS publish (path=$path, session=${it.sessionId}, msg=$msg)")
    synchronized(it) {
      try {
        if (it.session.isOpen)
          it.send(msg)
      } catch (ex: Throwable) { /* ignore errors */ }
    }
  }
}