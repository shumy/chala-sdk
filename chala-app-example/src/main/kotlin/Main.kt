package net.chala.app.example

import net.chala.ChalaNode

import net.chala.api.ChalaChainSpi
import net.chala.api.Tx
import net.chala.store.H2InMemoryConfig

class TestChain : ChalaChainSpi {
  override fun submmit(dto: Tx) {
    // TODO("Not yet implemented")
  }

  override fun onStartBlock(start: (Long) -> Unit) {
    // TODO("Not yet implemented")
  }

  override fun onCommitTx(tx: (Tx) -> Unit) {
    // TODO("Not yet implemented")
  }

  override fun onCommitBlock(commit: (Long) -> String) {
    // TODO("Not yet implemented")
  }

  override fun onRollbackBlock(rollback: (Long) -> Unit) {
    // TODO("Not yet implemented")
  }
}

fun main() {
  ChalaNode.setup(TestChain(), H2InMemoryConfig())
}