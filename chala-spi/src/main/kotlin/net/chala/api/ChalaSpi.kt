package net.chala.api

interface ChalaSpi {
  // should start DB tx
  fun onStart(start: () -> Unit): Unit

  fun <DTO> onCheckTx(tx: (Tx<DTO>) -> Unit): Unit
  fun <DTO> onProcessTx(tx: (Tx<DTO>) -> Unit): Unit

  // should commit DB tx and return AppState
  fun onCommit(commit: () -> String): Unit
  fun onRollback(rollback: () -> Unit): Unit
}

class Tx<DTO> (
  val value: DTO
)