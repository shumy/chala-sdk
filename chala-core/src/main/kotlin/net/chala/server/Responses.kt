package net.chala.server

import java.time.LocalDateTime

sealed class ChalaResponse(val cmdId: String?, val status: Int) {
  val timestamp = LocalDateTime.now()
}

class SubmittedResponse(cmdId: String): ChalaResponse(cmdId, 202)
class CommittedResponse(cmdId: String): ChalaResponse(cmdId, 200)

class BadInputResponse(val cause: String?): ChalaResponse(null, 400)
class BadFieldConstraintResponse(val field: String, val cause: String?): ChalaResponse(null, 400)
class ChainErrorResponse(cmdId: String, status: Int, val cause: String?): ChalaResponse(cmdId, status)