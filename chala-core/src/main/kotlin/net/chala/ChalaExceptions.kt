package net.chala

import java.lang.RuntimeException

private const val BASE_URL = "http://localhost:8080"

enum class Bug(val msg: String, val uri: String) {
  SAVE(
    "Context ${chainContext.get()} cannot save entities. Please submit the request to the chain via ChalaNode.submit(..)",
    "/bugs#save"
  ),

  CHECK(
    "Context ${chainContext.get()} cannot access the database. Please remove all database calls from all ChalaRequest.check(..)",
    "/bugs#check"
  ),

  SUBMIT(
    "Context ${chainContext.get()} cannot submit to chain. Please remove all ChalaNode.submit() calls from all ChalaRequest.[check(..) | validate(..) | commit(..)]",
    "/bugs#check"
  );

  fun report(): Nothing = throw ChalaBugException(msg, "$BASE_URL/$uri")
}

data class ChalaConfigException(override val message: String) : RuntimeException()

data class ChalaBugException(override val message: String, val url: String) : RuntimeException()

data class ChalaException(override val message: String) : RuntimeException()