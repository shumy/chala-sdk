package net.chala.app.example

import kotlinx.serialization.Serializable
import net.chala.ChalaNode
import net.chala.ChalaRequest
import net.chala.api.ChalaChainSpi
import net.chala.api.Tx
import net.chala.store.H2InMemoryConfig

class TestChain : ChalaChainSpi {
  override fun submmit(tx: Tx) {
    println("Calling Chain.submmit: ${tx.data.decodeToString()}")
  }

  override fun onStartBlock(start: (Long) -> Unit) {
    println("Registering Chain.onStartBlock")
  }

  override fun onCommitTx(tx: (Tx) -> Unit) {
    println("Registering Chain.onCommitTx")
  }

  override fun onCommitBlock(commit: (Long) -> String) {
    println("Registering Chain.onCommitBlock")
  }

  override fun onRollbackBlock(rollback: (Long) -> Unit) {
    println("Registering Chain.onRollbackBlock")
  }
}

@Serializable
data class TestData(val name: String)

class TestRequest(override val data: TestData) : ChalaRequest() {
  override fun check() {
    println("Calling TestRequest.check: $data")
  }

  override fun validate() {
    println("Calling TestRequest.validate: $data")
  }

  override fun commit() {
    println("Calling TestRequest.commit: $data")
  }
}

fun main() {
  ChalaNode.setup(TestChain(), H2InMemoryConfig())

  TestData.serializer()
  val data = TestData("Alex")
  ChalaNode.submit(TestRequest(data))
}