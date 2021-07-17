package net.chala

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import net.chala.annotation.Command
import net.chala.conf.ChalaConfiguration
import net.chala.model.AppState
import net.chala.model.DataPacket
import net.chala.server.ChalaServer
import net.chala.store.ChalaStore
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.locks.ReentrantLock

private val LOGGER = LoggerFactory.getLogger(ChalaNode::class.java)

class ChalaNode private constructor(internal val store: ChalaStore, internal val server: ChalaServer, val config: ChalaConfiguration) {
  companion object {
    private var NODE: ChalaNode? = null
    val node: ChalaNode by lazy {
      NODE ?: throw ChalaException("Uninitialized Chala node! Please run ChalaNode.setup(..)")
    }

    fun setup(config: ChalaConfiguration) {
      if (NODE != null)
        throw ChalaException("Chala node is already initialized!")

      println("-------------------------------------- ChalaNode --------------------------------------")
      val store = ChalaStore(config.storeConf)
      LOGGER.info("Chala store configured.")

      val server = ChalaServer(config)
      LOGGER.info("Chala server configured.")

      NODE = ChalaNode(store, server, config)
      node.loadState()
      node.context.set(RunContext.CLIENT)
      LOGGER.info("Chala node is UP.")
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun submit(command: ChalaCommand) {
      if (node.context.get() != RunContext.CLIENT)
        Bug.SUBMIT.report()

      // disable database access with CHECK context
      node.context.set(RunContext.CHECK)
        command.check()
      node.context.set(RunContext.CLIENT)

      val requestName = command.javaClass.canonicalName
      val commandInfo = node.config.commands[requestName] ?:
        throw ChalaException("$requestName must be annotated with ${Command::class.qualifiedName}!")

      val data = ProtoBuf.encodeToByteArray(commandInfo.serializer, command.data)
      val packet = ProtoBuf.encodeToByteArray(DataPacket(requestName, data))

      // TODO: sign packet -> tx with node key
      val tx = packet

      node.config.chain.submmit(tx)
    }
  }

  // -------------------------------------------------------------------------
  // exclusive chain operations ----------------------------------------------
  // -------------------------------------------------------------------------
  private val pendingTx = mutableListOf<ByteArray>()
  private var height: Long = 0
  private lateinit var state: ByteArray
  private lateinit var command: ChalaCommand
  private lateinit var data: Any

  private val locker = ReentrantLock()
  internal val context = ThreadLocal<RunContext>()

  init {
    config.chain.onStartBlock = this::startBlock
    config.chain.onValidateTx = this::validateTx
    config.chain.onCommitTx = this::commitTx
    config.chain.onCommitBlock = this::commitBlock
    config.chain.onRollbackBlock = this::rollbackBlock
  }

  internal fun loadState() {
    store.getSession().let {
      val query = it.createQuery("from AppState order by height desc")
      query.maxResults = 1
      val appState = query.uniqueResult() as AppState?
      state = appState?.state ?: "base-line:${UUID.randomUUID()}".toByteArray()
    }
  }

  fun startClientRequest() {
    context.set(RunContext.CLIENT)
  }

  private fun startBlock(bHeight: Long) {
    locker.lock()
    height = bHeight
    context.set(RunContext.CHAIN)
    store.getSession().beginTransaction()
  }

  @OptIn(ExperimentalSerializationApi::class)
  private fun validateTx(tx: ByteArray): Boolean {
    try {
      // TODO: check signature of tx and unpack
      val packet = ProtoBuf.decodeFromByteArray<DataPacket>(tx)
      val commandInfo = config.commands[packet.type]
        ?: throw ChalaException("${packet.type} must be annotated with ${Command::class.qualifiedName}!")

      data = ProtoBuf.decodeFromByteArray(commandInfo.serializer, packet.data)
      command = commandInfo.constructor.call(data)
    } catch (ex: Throwable) {
      // No changes in the hibernate session were performed. Abort the current tx and proceed to the next one
      LOGGER.warn("Failed to decode transaction: ${ex.message}")
      return false
    }

    try {
      // disable database access with CHECK context
      context.set(RunContext.CHECK)
      command.check()
    } catch (ex: Throwable) {
      // No changes in the hibernate session were performed. Abort the current tx and proceed to the next one
      LOGGER.warn("Failed to check transaction: ${ex.message}")
      return false
    }

    try {
      // disable database changes with VALIDATE context
      context.set(RunContext.VALIDATE)
      command.validate()
    } catch (ex: Throwable) {
      // No changes in the hibernate session were performed. Abort the current tx and proceed to the next one
      LOGGER.warn("Failed to validate transaction: ${ex.message}")
      return false
    }

    pendingTx.add(tx)
    return true
  }

  private fun commitTx() {
    try {
      // enable database changes with COMMIT context
      context.set(RunContext.COMMIT)
      command.commit()
    } catch (ex: Throwable) {
      // Can contain pending changes in the hibernate session. Abort the entire block
      // Possible exception at this phase could be related to DB connectivity
      throw ChalaException("Unable to commit transaction (aborting block): ${ex.message}")
    }
  }

  private fun commitBlock(): ByteArray {
    val digester = MessageDigest.getInstance("SHA-256")
    val newState = digester.digest(pendingTx.reduce { acc, bytes -> acc + bytes })

    store.getSession().let {
      val appState = AppState(height, newState)
      it.persist(appState)
      it.transaction.commit()
      it.close()
      LOGGER.info("Block committed for $appState")

      // prepare/clear context for the next block
      state = newState
      pendingTx.clear()
      store.chainSession.set(null)
    }

    context.set(RunContext.CLIENT)
    locker.unlock()

    return state
  }

  private fun rollbackBlock(ex: Throwable) {
    LOGGER.error("Forced block rollback at height: $height with reason ${ex.message}")
    store.getSession().let {
      try {
        it.transaction.rollback()
        it.close()
      } catch (ex: Throwable) {
        // ignore this! Nothing we can do here
      } finally {
        // prepare/clear context for the next block
        pendingTx.clear()
        store.chainSession.set(null)

        context.set(RunContext.CLIENT)
        locker.unlock()
      }
    }
  }
}

enum class RunContext {
  CLIENT, CHAIN, CHECK, VALIDATE, COMMIT
}