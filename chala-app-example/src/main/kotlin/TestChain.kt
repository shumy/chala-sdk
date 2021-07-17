package net.chala.app.example

import net.chala.api.ChalaChainSpi
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class TestChain(private val txPerBlock: Int) : ChalaChainSpi {
  override lateinit var onStartBlock: (Long) -> Unit
  override lateinit var onValidateTx: (ByteArray) -> Boolean
  override lateinit var onCommitTx: () -> Unit
  override lateinit var onCommitBlock: () -> ByteArray
  override lateinit var onRollbackBlock: (Throwable) -> Unit

  private val bNumber = AtomicLong(0L)
  private val txPool = ConcurrentLinkedQueue<ByteArray>()

  private val interval = 2000L
  private val scheduler = Executors.newScheduledThreadPool(1)

  private val task = Runnable {
    println("Check txPool: ${txPool.size} - ${LocalDateTime.now()}")
    if (txPool.size != txPerBlock)
      return@Runnable

    val number = bNumber.addAndGet(1)
    println("  Chain process block $number")

    try {
      println("  Chain.onStartBlock")
      onStartBlock(number)

      txPool.forEach {
        println("    Chain.onValidateTx")
        if (onValidateTx(it)) {
          println("    Chain.onCommitTx")
          onCommitTx()
        }
      }

      println("  Chain.onCommitBlock")
      onCommitBlock()
    } catch (ex: Throwable) {
      println("  Chain.onRollbackBlock")
      onRollbackBlock(ex)
    } finally {
      txPool.clear()
    }
  }

  init {
    scheduler.scheduleWithFixedDelay(task, 0L, interval, TimeUnit.MILLISECONDS)
  }

  override fun submmit(tx: ByteArray) {
    println("Chain.submmit: ${tx.decodeToString()}")
    txPool.add(tx)
  }
}