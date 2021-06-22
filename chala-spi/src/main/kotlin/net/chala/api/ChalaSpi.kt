package net.chala.api

interface ChalaChainSpi {
  var onStartBlock: (Long) -> Unit
  var onValidateTx: (ByteArray) -> Boolean
  var onCommitTx: () -> Unit
  var onCommitBlock: () -> ByteArray
  var onRollbackBlock: (Throwable) -> Unit

  fun submmit(tx: ByteArray): Unit
}