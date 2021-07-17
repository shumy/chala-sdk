package net.chala

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import net.chala.api.Command
import net.chala.conf.ChalaConfiguration
import net.chala.server.ChainErrorResponse
import net.chala.server.ChalaServer
import net.chala.server.CommittedResponse
import net.chala.service.AppState
import net.chala.store.ChalaStore
import net.chala.utils.shaDigest
import net.chala.utils.shaFingerprint
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val LOGGER = LoggerFactory.getLogger(ChalaNode::class.java)

@Serializable
internal open class DataPacket(val type: String, val data: ByteArray)

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
      chainContext.set(RunContext.CLIENT)
      LOGGER.info("Chala node is UP.")
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun submit(command: ChalaCommand): String {
      if (chainContext.get() != RunContext.CLIENT)
        Bug.SUBMIT.report()

      // disable database access with CHECK context
      chainContext.set(RunContext.CHECK)
        command.check()
      chainContext.set(RunContext.CLIENT)

      val requestName = command.javaClass.canonicalName
      val commandInfo = node.config.commands[requestName] ?:
        throw ChalaException("$requestName must be annotated with ${Command::class.qualifiedName}!")

      val data = ProtoBuf.encodeToByteArray(commandInfo.serializer, command.data)
      val packet = ProtoBuf.encodeToByteArray(DataPacket(requestName, data))

      // TODO: sign packet -> tx with node key
      val tx = packet

      node.config.chain.submmit(tx)
      return shaFingerprint(tx)
    }
  }

  // -------------------------------------------------------------------------
  // exclusive chain operations ----------------------------------------------
  // -------------------------------------------------------------------------
  private var height: Long = 0
  private lateinit var appState: AppState

  private val pendingTx = mutableListOf<ByteArray>()
  private lateinit var command: ChalaCommand
  private lateinit var data: Any

  private val locker = ReentrantLock()

  init {
    config.chain.onStartBlock = this::startBlock
    config.chain.onValidateTx = this::validateTx
    config.chain.onCommitTx = this::commitTx
    config.chain.onCommitBlock = this::commitBlock
    config.chain.onRollbackBlock = this::rollbackBlock
  }

  internal fun getState() = locker.withLock { appState }

  internal fun loadState() {
    store.getSession().let {
      val query = it.createQuery("from AppState order by height desc")
      query.maxResults = 1

      val dbAppState = query.uniqueResult() as AppState?
      appState = dbAppState ?: run {
        val initialState = dbAppState?.state ?: "init-state:${UUID.randomUUID()}".toByteArray()
        AppState(0L, initialState, 0)
      }
    }
  }

  private fun startBlock(bHeight: Long) {
    locker.lock()
    height = bHeight
    chainContext.set(RunContext.CHAIN)
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
      reportTxError(tx, 400, "Failed to decode cmd: ${ex.message}")
      return false
    }

    try {
      // disable database access with CHECK context
      chainContext.set(RunContext.CHECK)
      command.check()
    } catch (ex: Throwable) {
      // No changes in the hibernate session were performed. Abort the current tx and proceed to the next one
      reportTxError(tx, 400, "Failed to check cmd: ${ex.message}")
      return false
    }

    try {
      // disable database changes with VALIDATE context
      chainContext.set(RunContext.VALIDATE)
      command.validate()
    } catch (ex: Throwable) {
      // No changes in the hibernate session were performed. Abort the current tx and proceed to the next one
      reportTxError(tx, 400, "Failed to validate cmd: ${ex.message}")
      return false
    }

    pendingTx.add(tx)
    return true
  }

  private fun commitTx() {
    try {
      // enable database changes with COMMIT context
      chainContext.set(RunContext.COMMIT)
      command.commit()
    } catch (ex: Throwable) {
      // Can contain pending changes in the hibernate session. Abort the entire block
      // Possible exception at this phase could be related to DB connectivity
      throw ChalaException("Unable to commit transaction (aborting block): ${ex.message}")
    }
  }

  private fun commitBlock(): ByteArray {
    val newState = if (pendingTx.isNotEmpty()) {
      shaDigest(pendingTx.reduce { acc, bytes -> acc + bytes })
    } else appState.state

    val newAppState = AppState(height, newState, pendingTx.size)
    store.getSession().let {
      it.persist(newAppState)
      it.transaction.commit()
      it.close()
      LOGGER.info("Block committed for $newAppState")
    }

    pendingTx.forEach { tx ->
      server.publish(CommittedResponse(shaFingerprint(tx)))
    }

    // prepare/clear context for the next block
    appState = newAppState
    pendingTx.clear()
    store.chainSession.set(null)

    chainContext.set(RunContext.CLIENT)
    locker.unlock()

    return appState.state
  }

  private fun rollbackBlock(ex: Throwable) = store.getSession().let {
    pendingTx.forEach { tx ->
      reportTxError(tx, 500, "Forced block rollback at height $height with reason: ${ex.message}")
    }

    try {
      it.transaction.rollback()
      it.close()
    } catch (ex: Throwable) {
      // ignore this! Nothing we can do here
    } finally {
      // prepare/clear context
      height -= 1
      pendingTx.clear()
      store.chainSession.set(null)

      chainContext.set(RunContext.CLIENT)
      locker.unlock()
    }
  }

  private fun reportTxError(tx: ByteArray, status: Int, cause: String) {
    LOGGER.warn(cause)
    server.publish(ChainErrorResponse(shaFingerprint(tx), status, cause))
  }
}