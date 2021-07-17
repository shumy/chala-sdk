package net.chala

open class ChalaRecord {
  fun save() = ChalaNode.node.store.save(this)
}

open class ChalaRepository<E> {
  fun findAll(): Sequence<E>
    = ChalaNode.node.store.find(type, "from ${type.simpleName}")

  fun findMany(query: String, vararg params: Any): Sequence<E>
    = ChalaNode.node.store.find(type, query, params)

  fun findOne(query: String, vararg params: Any): E?
    = findMany(query, params).first()

  fun findById(id: Long): E?
    = ChalaNode.node.store.findById(type, id)

  @Suppress("UNCHECKED_CAST")
  private val type: Class<E> by lazy {
    this::class.java.declaringClass as Class<E>
  }
}

interface ChalaCommand {
  val data: Any

  fun check(): Unit
  fun validate(): Unit
  fun commit(): Unit
}