package net.chala.conf

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.chala.ChalaCommand
import net.chala.ChalaException
import net.chala.annotation.Command
import net.chala.annotation.Endpoint
import net.chala.annotation.Query
import net.chala.api.ChalaChainSpi
import net.chala.store.StoreConfig
import org.slf4j.LoggerFactory
import javax.persistence.Entity
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.*

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

      LOGGER.info("Scanning for classes with @${Entity::class.qualifiedName}:")
      val jpaClasses = allClasses.filterClassByAnnotation(Entity::class)

      // TODO: optimize size of keys (it.qualifiedName). This is sent in the package.
      LOGGER.info("Scanning for classes with @${Command::class.qualifiedName}:")
      val commands = allClasses
        .filterClassByAnnotation(Command::class)
        .map { it.convertToChalaCommand() }
        .associate { it.qualifiedName!! to CommandInfo(it.findAnnotation()!!, it.commandConstructor(), it.commandSerializer()) }

      LOGGER.info("Scanning for classes with @${Query::class.qualifiedName}:")
      val queries = allClasses
        .filterClassByAnnotation(Query::class)
        .map { QueryInfo(it.findAnnotation()!!, it.queryConstructor(), it.queryEndpoints()) }

      return ChalaConfiguration(jpaClasses, commands, queries)
    }
  }

  lateinit var storeConf: StoreConfig
  lateinit var chain: ChalaChainSpi
}

// --------------------------------------------------------------------------------------
// @Command
// --------------------------------------------------------------------------------------
@Suppress("UNCHECKED_CAST")
private fun KClass<*>.convertToChalaCommand(): KClass<ChalaCommand> {
  if (!isSubclassOf(ChalaCommand::class))
    throw ChalaException("$qualifiedName needs to implement ${ChalaCommand::class.qualifiedName}!")

  return this as KClass<ChalaCommand>
}

@Suppress("UNCHECKED_CAST")
private fun KClass<ChalaCommand>.commandSerializer(): KSerializer<Any> {
  val dataField = commandDataField()
  if (!dataField.second.hasAnnotation<Serializable>())
    throw ChalaException("${dataField.second.qualifiedName} must be annotated with ${Serializable::class.qualifiedName}!")

  // this cannot fail if dataPropertyType is annotated with Serializable
  val companion = dataField.second.companionObjectInstance!!
  val serializerMethod = companion.javaClass.getMethod("serializer")
  return serializerMethod.invoke(companion) as KSerializer<Any>
}

private fun KClass<ChalaCommand>.commandConstructor(): KFunction<ChalaCommand> {
  val dataField = commandDataField()

  val defaultConstructor = constructors.first { it.name == "<init>" }
  if (defaultConstructor.parameters.size != 1 && defaultConstructor.parameters[0].type != dataField.first)
    throw ChalaException("$qualifiedName must have a default constructor (data: ${dataField.second.qualifiedName})!")

  return defaultConstructor
}

private fun KClass<ChalaCommand>.commandDataField(): Pair<KType, KClass<*>> {
  val dataType = memberProperties.first { it.name == "data" }
  val dataClass = dataType.returnType.classifier as KClass<*>
  return Pair(dataType.returnType, dataClass)
}

// --------------------------------------------------------------------------------------
// @Query
// --------------------------------------------------------------------------------------
private fun KClass<*>.queryConstructor(): KFunction<Any> {
  val defaultConstructor = constructors.first { it.name == "<init>" }
  if (defaultConstructor.parameters.isNotEmpty())
    throw ChalaException("$qualifiedName must have an empty default constructor!")

  return defaultConstructor
}

private fun KClass<*>.queryEndpoints(): List<EndpointInfo> =
  memberFunctions
    .filterMethodByAnnotation(Endpoint::class)
    .map { EndpointInfo(it.findAnnotation()!!, it) }