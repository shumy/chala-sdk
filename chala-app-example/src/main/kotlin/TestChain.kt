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
  private val block = ConcurrentLinkedQueue<ByteArray>()

  private val initialDelay = 0L
  private val interval = 3000L
  private val scheduler = Executors.newScheduledThreadPool(1)

  private val task = Runnable {
    if (block.size == 0) {
      println("  Chain, no tx to process: ${LocalDateTime.now()}")
      return@Runnable
    }

    val number = bNumber.addAndGet(1)
    println("  Chain process block $number")

    try {
      println("  Chain.onStartBlock")
      onStartBlock(number)

      block.forEach {
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
      block.clear()
    }
  }

  init {
    scheduler.scheduleWithFixedDelay(task, initialDelay, interval, TimeUnit.MILLISECONDS)
  }

  override fun submmit(tx: ByteArray) {
    println("Chain.submmit: ${tx.decodeToString()}")
    block.add(tx)
  }
}