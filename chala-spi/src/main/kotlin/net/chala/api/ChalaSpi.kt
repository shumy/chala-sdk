package net.chala.api

interface ChalaSpi {
  fun submmit(dto: Tx): Unit

  // should start DB tx
  fun onStart(start: () -> Unit): Unit

  fun onProcessTx(tx: (Tx) -> Unit): Unit

  // should commit DB tx and return AppState
  fun onCommit(commit: () -> String): Unit
  fun onRollback(rollback: () -> Unit): Unit
}

class Tx (val value: ByteArray, val signature: ByteArray)