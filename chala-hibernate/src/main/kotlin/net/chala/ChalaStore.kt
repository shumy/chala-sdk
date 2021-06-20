package net.chala

import net.chala.api.ChalaSpi
import net.chala.api.Tx
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import kotlin.streams.asSequence

class ChalaStore(private val spi: ChalaSpi, private val sf: SessionFactory) {
  companion object {
    private val LOGGER = LoggerFactory.getLogger(ChalaStore::class.java)

    var STORE: ChalaStore? = null
      private set

    fun setup(spi: ChalaSpi) {
      val conf = Configuration().apply {
        //addAnnotatedClass (org.gradle.Person.class);
        setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        setProperty("hibernate.connection.url", "jdbc:mysql://localhost:3306/hibernate");
        setProperty("hibernate.connection.username", "root");
        setProperty("hibernate.connection.password", "123");
        setProperty("hibernate.connection.autoReconnect", "true")

        setProperty("hibernate.hbm2ddl.auto", "update")
        setProperty("hibernate.connection.pool_size", "10")
        setProperty("show_sql", "true")

        //setProperty("connection.provider_class", "org.hibernate.connection.C3P0ConnectionProvider");
        //setProperty("c3p0.min_size", "5");
        //setProperty("c3p0.max_size", "20");
        //setProperty("c3p0.timeout", "1800");
        //setProperty("c3p0.max_statements", "100");
        //setProperty("hibernate.c3p0.testConnectionOnCheckout", "true");
      }

      val builder = StandardServiceRegistryBuilder().applySettings(conf.properties)
      val sf = conf.buildSessionFactory(builder.build())
      
      LOGGER.info("SessionFactory created.")
      STORE = ChalaStore(spi, sf)
    }

    fun submmit(dto: ChalaRequest) {
      // TODO: encode dto -> tx
      // TODO: sign tx with node key

      // STORE.spi.submmit(tx)
    }
  }

  private val session = AtomicReference<Session>()

  internal fun save(entity: Any) = session.get().persist(entity)

  internal fun <E> find(type: Class<E>, query: String, vararg params: Any): Sequence<E> {
    val q = session.get().createQuery(query, type)
    params.forEachIndexed() { i, p -> q.setParameter(i, p) }
    return q.stream().asSequence()
  }

  internal fun <E> findOne(type: Class<E>, query: String, vararg params: Any): E? =
    find(type, query, params).first()

  internal fun <E> findById(type: Class<E>, id: Any): E? =
    session.get().find(type, id)


  init {
    spi.onStart(this::start)
    spi.onProcessTx(this::process)
    spi.onCommit(this::commit)
    spi.onRollback(this::rollback)
  }

  private fun start() = sf.openSession().let {
    it.beginTransaction()
    session.set(it)
  }

  private fun process(tx: Tx) {
    // TODO: decode tx -> dto

    // dto.check()
    // dto.process()
  }

  private fun commit(): String {
    session.get().let {
      it.transaction.commit()
      it.close()
    }

    // TODO: set app state from all tx
    return ""
  }

  private fun rollback() = session.get().let {
    it.transaction.rollback()
    it.close()
  }
}