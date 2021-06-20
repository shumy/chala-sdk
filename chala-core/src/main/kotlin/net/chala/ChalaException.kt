package net.chala

import java.lang.RuntimeException

private const val BASE_URL = "http://localhost:8080"

enum class Bug(val msg: String, val uri: String) {
  SAVE(
    "Context ${ChalaNode.node.context.get()} cannot save entities. Please submit the request to the chain via ChalaNode.submit(..)",
    "/bugs#save"
  ),

  CHECK(
    "Context ${ChalaNode.node.context.get()} cannot access the database. Please remove all database calls from all ChalaRequest.check(..)",
    "/bugs#check"
  ),

  SUBMIT(
    "Context ${ChalaNode.node.context.get()} cannot submit to chain. Please remove all ChalaNode.submit() calls from all ChalaRequest.[check(..) | validate(..) | commit(..)]",
    "/bugs#check"
  );

  fun report(): Nothing = throw ChalaBugException(msg, "$BASE_URL/$uri")
}

data class ChalaBugException(val msg: String, val url: String) : RuntimeException(msg)

data class ChalaException(val msg: String) : RuntimeException(msg)