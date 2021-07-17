package net.chala.conf

import net.chala.ChalaConfigException
import net.chala.api.ChalaChainSpi
import net.chala.api.Command
import net.chala.api.Query
import net.chala.service.AppState
import net.chala.service.ChainQuery
import net.chala.store.StoreConfig
import net.chala.utils.filterClassByAnnotation
import net.chala.utils.getClasses
import org.slf4j.LoggerFactory
import javax.persistence.Entity
import kotlin.reflect.KClass

private val LOGGER = LoggerFactory.getLogger(ChalaConfiguration::class.java)

class ChalaConfiguration private constructor(
  val jpaClasses: List<KClass<*>>,
  val commands: Map<String, CommandInfo>,
  val queries: List<QueryInfo>
) {

  companion object {
    fun scan(vararg scanPackages: String): ChalaConfiguration {
      LOGGER.info("Scanning packages [${scanPackages.joinToString()}]:")
      val allClasses = scanPackages.flatMap { getClasses(it) }

      LOGGER.info("Scanning for classes with @${Entity::class.simpleName}:")
      val jpaClasses = allClasses
        .filterClassByAnnotation(Entity::class)
        .plus(AppState::class)
        .onEach { LOGGER.info("Found JPA entity ${it.qualifiedName}") }

      // TODO: optimize size of keys (it.qualifiedName). This is sent in the package.
      LOGGER.info("Scanning for classes with @${Command::class.simpleName}:")
      val commands = allClasses
        .filterClassByAnnotation(Command::class)
        .onEach { LOGGER.info("Found command class ${it.qualifiedName}") }
        .map { it.convertToChalaCommand() }
        .associate { it.qualifiedName!! to it.mapToCommandInfo() }

      LOGGER.info("Scanning for classes with @${Query::class.simpleName}:")
      val queries = allClasses
        .filterClassByAnnotation(Query::class)
        .plus(ChainQuery::class)
        .onEach { LOGGER.info("Found query class ${it.qualifiedName}") }
        .map { it.mapToQueryInfo() }

      // check for overridden paths
      val allPaths = queries
        .flatMap { it.endpoints.map(EndpointInfo::path) }
        .plus(commands.values.map { it.path })

      val checkPaths = mutableSetOf<String>()
      for (path in allPaths) {
        if (checkPaths.contains(path))
          throw ChalaConfigException("Path $path already exists!")
        checkPaths.add(path)
      }

      return ChalaConfiguration(jpaClasses, commands, queries)
    }
  }

  lateinit var storeConf: StoreConfig
  lateinit var chain: ChalaChainSpi
}