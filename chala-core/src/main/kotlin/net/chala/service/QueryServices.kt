package net.chala.service

import net.chala.ChalaNode
import net.chala.api.Endpoint
import net.chala.api.Query
import java.util.*

@Query("/chain", document = "chain.yaml")
class StatusQuery {
  class State(val block: Long, val hash: String)

  @Endpoint("/state")
  fun state(): State {
    val appState = ChalaNode.node.getState()
    val hash = String(Base64.getEncoder().encode(appState.state))
    return State(appState.height, hash)
  }
}