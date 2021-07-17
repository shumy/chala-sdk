package net.chala.service

import net.chala.ChalaNode
import net.chala.api.Endpoint
import net.chala.api.Query
import net.chala.utils.shaFingerprint

@Query("/chain", document = "chain.yaml")
class ChainQuery {
  class State(val block: Long, val hash: String, val txNumber: Int)

  @Endpoint("/current")
  fun current(): State {
    val appState = ChalaNode.node.getState()
    return State(appState.height, shaFingerprint(appState.state), appState.txNumber)
  }
}