package net.chala

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import net.chala.api.ChalaChainSpi
import net.chala.api.Tx
import net.chala.model.AppState
import net.chala.model.DataPacket
import net.chala.store.ChalaStore
import net.chala.store.StoreConfig
import org.slf4j.LoggerFactory
import java.util.*

class ChalaNode(private val chain: ChalaChainSpi, val store: ChalaStore) {
  companion object {
    private val LOGGER = LoggerFactory.getLogger(ChalaNode::class.java)

    private var NODE: ChalaNode? = null
    val node: ChalaNode by lazy {
      NODE ?: throw ChalaException("Uninitialized ChalaNode! Please run ChalaNode.setup(..)")
    }

    fun setup(chain: ChalaChainSpi, config: StoreConfig) {
      if (NODE != null)
        throw ChalaException("ChalaNode is already initialized!")

      val store = ChalaStore(config)

      println("-------------------------------------- ChalaSDK --------------------------------------")
      LOGGER.info("ChalaStore configured.")

      NODE = ChalaNode(chain, store)
      node.loadState()
      node.context.set(RunContext.CLIENT)
      LOGGER.info("ChalaNode configured.")
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun submit(request: ChalaRequest) {
      if (node.context.get() != RunContext.CLIENT)
        Bug.SUBMIT.report()

      // disable database access with CHECK context
      node.context.set(RunContext.CHECK)
        request.check()
      node.context.set(RunContext.CLIENT)

      // TODO: encode failure ??
      val data = ProtoBuf.encodeToByteArray(request.serializer, request.data)
      val packet = ProtoBuf.encodeToByteArray(DataPacket(request.javaClass.canonicalName, data))

      // TODO: sign tx with node key

      node.chain.submmit(Tx(packet))
    }
  }

  // -------------------------------------------------------------------------
  // exclusive chain operations ----------------------------------------------
  // -------------------------------------------------------------------------
  private lateinit var state: String
  internal val context = ThreadLocal<RunContext>()

  init {
    chain.onStartBlock(this::startBlock)
    chain.onCommitTx(this::commitTx)
    chain.onCommitBlock(this::commitBlock)
    chain.onRollbackBlock(this::rollbackBlock)
  }

  internal fun loadState() {
    store.getSession().let {
      val query = it.createQuery("from AppState order by height desc")
      query.maxResults = 1
      val appState = query.uniqueResult() as AppState?
      state = appState?.state ?: "base-line:${UUID.randomUUID()}"
    }
  }

  private fun startBlock(height: Long) {
    context.set(RunContext.CHAIN)
    store.getSession().let {
      it.beginTransaction()
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  private fun commitTx(tx: Tx) {
    // TODO: decode failure ??
    val packet = ProtoBuf.decodeFromByteArray<DataPacket>(tx.data)
    val type = Class.forName(packet.type)

    // TODO: how to get dto with packet and type ?

    try {
      // disable database access with CHECK context
      context.set(RunContext.CHECK)
      //dto.check()
    } catch (ex: Throwable) {
      // No changes in the hibernate session were performed. Abort the current tx and proceed to the next one
      LOGGER.warn("Failed to check transaction. Ignore and proceed to the next one.")
      return
    }

    try {
      // disable database changes with VALIDATE context
      context.set(RunContext.VALIDATE)
      //dto.validate()
    } catch (ex: Throwable) {
      // No changes in the hibernate session were performed. Abort the current tx and proceed to the next one
      LOGGER.warn("Failed to validate transaction. Ignore and proceed to the next one.")
      return
    }

    try {
      // enable database changes with COMMIT context
      context.set(RunContext.COMMIT)
      //dto.commit()

      // TODO: update state var

    } catch (ex: Throwable) {
      // Can contain pending changes in the hibernate session. Abort the entire block
      // Possible exception at this phase could be related to DB connectivity
      throw ChalaException("Unable to commit transaction due to: ${ex.message}. Problem could be related to DB connectivity!")
    }
  }

  private fun commitBlock(height: Long): String {
    context.set(RunContext.CHAIN)
    store.getSession().let {
      try {
        it.persist(AppState(height, state))
        it.transaction.commit()
      } catch (ex: Throwable) {
        LOGGER.error("Failed to commit transaction. Rollback block at height: $height")
        it.transaction.rollback()
      } finally {
        it.close()
        store.chainSession.set(null)
      }
    }

    return state
  }

  private fun rollbackBlock(height: Long) {
    context.set(RunContext.CHAIN)
    store.getSession().let {
      try {
        LOGGER.error("Forced block rollback at height: $height")
        it.transaction.rollback()
      } catch (ex: Throwable) {
        // ignore this! Nothing we can do here
      } finally {
        it.close()
        store.chainSession.set(null)
      }
    }
  }
}

enum class RunContext {
  CLIENT, CHAIN, CHECK, VALIDATE, COMMIT
}