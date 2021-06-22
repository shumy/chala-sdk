package net.chala.app.example

import kotlinx.serialization.Serializable
import net.chala.ChalaNode
import net.chala.ChalaRequest
import net.chala.annotation.Command
import net.chala.api.ChalaChainSpi
import net.chala.conf.ChalaConfiguration
import net.chala.store.H2InMemoryConfig
import java.util.concurrent.ConcurrentLinkedQueue

class TestChain(private val txPerBlock: Int) : ChalaChainSpi {
  override lateinit var onStartBlock: (Long) -> Unit
  override lateinit var onValidateTx: (ByteArray) -> Boolean
  override lateinit var onCommitTx: () -> Unit
  override lateinit var onCommitBlock: (Long) -> ByteArray
  override lateinit var onRollbackBlock: (Long, Throwable) -> Unit

  private var bNumber = 0L
  private val block = ConcurrentLinkedQueue<ByteArray>()

  override fun submmit(tx: ByteArray) {
    println("Chain.submmit: ${tx.decodeToString()}")

    block.add(tx)
    if (block.size < txPerBlock)
      return

    bNumber++
    try {
      println("  Chain.onStartBlock")
      onStartBlock(bNumber)

      block.forEach {
        println("    Chain.onValidateTx")
        if (onValidateTx(it)) {
          println("    Chain.onCommitTx")
          onCommitTx()
        }
      }

      println("  Chain.onCommitBlock")
      onCommitBlock(bNumber)
    } catch (ex: Throwable) {
      println("  Chain.onRollbackBlock")
      onRollbackBlock(bNumber, ex)
    } finally {
      block.clear()
    }
  }
}

@Serializable
data class TestData(val name: String, val index: Int)

@Command
class TestRequest(override val data: TestData) : ChalaRequest {
  override fun check() {
    println("      TestRequest.check: $data")
  }

  override fun validate() {
    println("      TestRequest.validate: $data")
    if (data.index % 3 == 2)
      throw RuntimeException("Simulated validation error!")
  }

  override fun commit() {
    println("      TestRequest.commit: $data")
  }
}

fun main() {
  val conf = ChalaConfiguration.scan("net.chala.app.example")
  conf.chain = TestChain(4)
  conf.storeConf = H2InMemoryConfig(conf.jpaClasses)

  ChalaNode.setup(conf)

  for (index in 0..8) {
    //thread {
      ChalaNode.node.startClientRequest()
      val alex = TestData("Name", index)
      ChalaNode.submit(TestRequest(alex))
    //}
  }
}