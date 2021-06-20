package net.chala.api

interface ChalaChainSpi {
  fun submmit(dto: Tx): Unit

  // should start a transaction session
  fun onStartBlock(start: (Long) -> Unit): Unit

  fun onCommitTx(tx: (Tx) -> Unit): Unit

  // should terminate the active transaction session
  fun onCommitBlock(commit: (Long) -> String): Unit
  fun onRollbackBlock(rollback: (Long) -> Unit): Unit
}

class Tx (val data: ByteArray, val signature: ByteArray? = null)