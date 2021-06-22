package net.chala.conf

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.chala.ChalaException
import net.chala.ChalaRequest
import net.chala.annotation.Command
import net.chala.api.ChalaChainSpi
import net.chala.store.StoreConfig
import org.slf4j.LoggerFactory
import java.io.File
import javax.persistence.Entity
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

private val LOGGER = LoggerFactory.getLogger(ChalaConfiguration::class.java)

class ChalaConfiguration private constructor(
  val jpaClasses: List<KClass<*>>,
  val serializers: Map<String, KSerializer<Any>>,
  val constructors: Map<String, KFunction<ChalaRequest>>
) {

  companion object {
    fun scan(vararg scanPackages: String): ChalaConfiguration {
      LOGGER.info("Scanning packages [${scanPackages.joinToString()}]:")
      val allClasses = scanPackages.flatMap { getClasses(it) }

      LOGGER.info("Scanning for classes with @${Entity::class.qualifiedName}:")
      val jpaClasses = allClasses.filterByAnnotation(Entity::class)

      LOGGER.info("Scanning for classes with @${Command::class.qualifiedName}:")
      val requestClasses = allClasses
        .filterByAnnotation(Command::class)
        .map { it.convertToChalaRequest() }

      // TODO: optimize size of keys (it.qualifiedName). This is sent in the package.
      val serializers = requestClasses
        .associate { it.qualifiedName!! to it.serializer() }

      val constructors = requestClasses
        .associate { it.qualifiedName!! to it.constructor() }

      return ChalaConfiguration(jpaClasses, serializers, constructors)
    }
  }

  lateinit var storeConf: StoreConfig
  lateinit var chain: ChalaChainSpi
}


private fun List<Class<*>>.filterByAnnotation(annotation: KClass<out Annotation>) =
  this.filter { it.isAnnotationPresent(annotation.java) }
    .map { it.kotlin }
    .onEach { LOGGER.info("Found ${it.qualifiedName}") }

private fun getClasses(packageName: String): Sequence<Class<*>> {
  val classLoader = Thread.currentThread().contextClassLoader!!
  val path = packageName.replace('.', '/')
  val resources = classLoader.getResources(path)

  return resources.asSequence()
    .map { File(it.toURI()) }
    .flatMap { findClasses(it, packageName) }
}

private fun findClasses(directory: File, packageName: String): List<Class<*>> {
  val classes = mutableListOf<Class<*>>()
  if (!directory.exists()) {
    return classes
  }

  val files = directory.listFiles()!!
  for (file in files) {
    if (file.isDirectory) {
      classes.addAll(findClasses(file, "$packageName.${file.name}"))
    } else if (file.name.endsWith(".class")) {
      classes.add(Class.forName("$packageName.${file.name.substring(0, file.name.length - 6)}"))
    }
  }

  return classes
}

@Suppress("UNCHECKED_CAST")
private fun KClass<*>.convertToChalaRequest(): KClass<ChalaRequest> {
  if (!isSubclassOf(ChalaRequest::class))
    throw ChalaException("$qualifiedName needs to implement ${ChalaRequest::class.qualifiedName}!")

  return this as KClass<ChalaRequest>
}

@Suppress("UNCHECKED_CAST")
private fun KClass<ChalaRequest>.serializer(): KSerializer<Any> {
  val dataField = getDataField()
  if (!dataField.second.hasAnnotation<Serializable>())
    throw ChalaException("${dataField.second.qualifiedName} must be annotated with ${Serializable::class.qualifiedName}!")

  // this cannot fail if dataPropertyType is annotated with Serializable
  val companion = dataField.second.companionObjectInstance!!
  val serializerMethod = companion.javaClass.getMethod("serializer")
  return serializerMethod.invoke(companion) as KSerializer<Any>
}

private fun KClass<ChalaRequest>.constructor(): KFunction<ChalaRequest> {
  val dataField = getDataField()

  val defaultConstructor = constructors.first { it.name == "<init>" }
  if (defaultConstructor.parameters.size != 1 && defaultConstructor.parameters[0].type != dataField.first)
    throw ChalaException("$qualifiedName must have a default constructor (data: ${dataField.second.qualifiedName})!")

  return defaultConstructor
}

private fun KClass<ChalaRequest>.getDataField(): Pair<KType, KClass<*>> {
  val dataType = memberProperties.first { it.name == "data" }
  val dataClass = dataType.returnType.classifier as KClass<*>
  return Pair(dataType.returnType, dataClass)
}