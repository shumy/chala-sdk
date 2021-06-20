package net.chala

private val store: ChalaStore by lazy {
  if (ChalaStore.STORE == null)
    throw RuntimeException("Uninitialized ChalaStore!")
  ChalaStore.STORE!!
}

open class ChalaRecord {
  fun save() = store.save(this)
}

open class ChalaRepository<E, ID : Any> {
  fun findAll(query: String, vararg params: Any): Sequence<E> = store.find(type, query, params)

  fun findOne(query: String, vararg params: Any): E? = findAll(query, params).first()
  fun findById(id: ID): E? = store.findById(type, id)

  @Suppress("UNCHECKED_CAST")
  private val type: Class<E> by lazy {
    this::class.java.declaringClass as Class<E>
  }
}

interface ChalaRequest {
  fun check(): Unit
  fun process(): Unit
}