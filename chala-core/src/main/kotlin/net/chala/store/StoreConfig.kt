package net.chala.store

import org.hibernate.cfg.Configuration
import kotlin.reflect.KClass

//setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
  //setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
  //setProperty("hibernate.connection.url", "jdbc:mysql://localhost:3306/hibernate");
  //setProperty("hibernate.connection.username", "root");
  //setProperty("hibernate.connection.password", "123");
  //setProperty("hibernate.connection.autoReconnect", "true")

  //setProperty("hibernate.hbm2ddl.auto", "update")
  //setProperty("hibernate.connection.pool_size", "10")
  //setProperty("show_sql", "true")

  //setProperty("connection.provider_class", "org.hibernate.connection.C3P0ConnectionProvider");
  //setProperty("c3p0.min_size", "5");
  //setProperty("c3p0.max_size", "20");
  //setProperty("c3p0.timeout", "1800");
  //setProperty("c3p0.max_statements", "100");
  //setProperty("hibernate.c3p0.testConnectionOnCheckout", "true");


abstract class StoreConfig(internal val conf: Configuration)

class H2InMemoryConfig(entities: List<KClass<*>>) : StoreConfig(
  Configuration().apply {
    setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
    setProperty("hibernate.connection.driver_class", "org.h2.Driver")
    setProperty("hibernate.connection.url", "jdbc:h2:mem:test")
    setProperty("hibernate.connection.username", "sa")
    setProperty("hibernate.connection.password", "")
    setProperty("hibernate.hbm2ddl.auto", "update")
    setProperty("hibernate.show_sql", "true")

    entities.forEach { addAnnotatedClass(it.java) }
  }
)