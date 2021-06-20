package net.chala

import kotlinx.serialization.KSerializer
import kotlin.reflect.full.companionObjectInstance

open class ChalaRecord {
  fun save() = ChalaNode.node.store.save(this)
}

open class ChalaRepository<E> {
  fun findAll(query: String, vararg params: Any): Sequence<E>
    = ChalaNode.node.store.find(type, query, params)

  fun findOne(query: String, vararg params: Any): E?
    = findAll(query, params).first()

  fun findById(id: Long): E?
    = ChalaNode.node.store.findById(type, id)

  @Suppress("UNCHECKED_CAST")
  private val type: Class<E> by lazy {
    this::class.java.declaringClass as Class<E>
  }
}


abstract class ChalaRequest {
  abstract val data: Any

  @Suppress("UNCHECKED_CAST")
  val serializer: KSerializer<Any> by lazy {
    val companion = data.javaClass.kotlin.companionObjectInstance!!
    val serializerMethod = companion.javaClass.getMethod("serializer")
    serializerMethod.invoke(companion) as KSerializer<Any>
  }

  abstract fun check(): Unit
  abstract fun validate(): Unit
  abstract fun commit(): Unit
}