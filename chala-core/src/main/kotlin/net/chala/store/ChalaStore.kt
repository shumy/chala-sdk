package net.chala.store

import net.chala.Bug
import net.chala.ChalaNode
import net.chala.RunContext
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import java.util.concurrent.atomic.AtomicReference
import kotlin.streams.asSequence

class ChalaStore(config: StoreConfig) {
  private var sf: SessionFactory

  init {
    val builder = StandardServiceRegistryBuilder().applySettings(config.conf.properties)
    sf = config.conf.buildSessionFactory(builder.build())
  }

  private val threadSessions = ThreadLocal<Session>()
  internal val chainSession = AtomicReference<Session>()

  internal fun save(entity: Any) {
    val ctx = ChalaNode.node.context.get()
    if (ctx == RunContext.CLIENT || ctx == RunContext.CHECK || ctx == RunContext.VALIDATE)
      Bug.SAVE.report()

    getSession().persist(entity)
  }

  internal fun <E> find(type: Class<E>, query: String, vararg params: Any): Sequence<E> {
    if (ChalaNode.node.context.get() == RunContext.CHECK)
      Bug.CHECK.report()

    val q = getSession().createQuery(query, type)
    params.forEachIndexed() { i, p -> q.setParameter(i, p) }
    return q.stream().asSequence()
  }

  internal fun <E> findById(type: Class<E>, id: Any): E? {
    if (ChalaNode.node.context.get() == RunContext.CHECK)
      Bug.CHECK.report()

    return getSession().find(type, id)
  }

  // -------------------------------------------------------------------------
  // JPA session management --------------------------------------------------
  // -------------------------------------------------------------------------
  internal fun getSession(): Session {
    if (ChalaNode.node.context.get() == RunContext.CHECK)
      Bug.CHECK.report()

    if (ChalaNode.node.context.get() == RunContext.CLIENT)
      return threadSessions.get() ?: run {
        val ss = sf.openSession()
        threadSessions.set(ss)
        ss
      }

    return chainSession.get()?: run {
      val ss = sf.openSession()
      chainSession.set(ss)
      ss
    }
  }

  /*internal fun resetSession(): Unit? {
    if (ChalaNode.node.context.get() == RunContext.CLIENT)
      return threadSessions.get()?.let {
        it.transaction?.rollback()
        it.close()
        threadSessions.set(null)
      }

    return chainSession.get()?.let {
      it.transaction?.rollback()
      it.close()
      chainSession.set(null)
    }
  }*/
}