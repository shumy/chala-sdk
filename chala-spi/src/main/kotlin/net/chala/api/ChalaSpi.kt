package net.chala.api

interface ChalaChainSpi {
  var onStartBlock: (Long) -> Unit
  var onValidateTx: (ByteArray) -> Boolean
  var onCommitTx: () -> Unit
  var onCommitBlock: (Long) -> ByteArray
  var onRollbackBlock: (Long, Throwable) -> Unit

  fun submmit(tx: ByteArray): Unit
}