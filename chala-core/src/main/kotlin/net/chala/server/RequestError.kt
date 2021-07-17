package net.chala.server

import java.time.LocalDateTime
import java.util.*

data class RequestError(val status: Int, val cause: String?) {
  val trace: String = UUID.randomUUID().toString()
  val timestamp: LocalDateTime = LocalDateTime.now()
}