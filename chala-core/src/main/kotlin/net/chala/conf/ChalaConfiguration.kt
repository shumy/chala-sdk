package net.chala.conf

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.chala.ChalaException
import net.chala.ChalaRequest
import net.chala.annotation.Request
import net.chala.api.ChalaChainSpi
import net.chala.store.StoreConfig
import org.slf4j.LoggerFactory
import java.io.File
import javax.persistence.Entity
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.javaType

private val LOGGER = LoggerFactory.getLogger(ChalaConfiguration::class.java)

class ChalaConfiguration private constructor(
  val jpaClasses: List<KClass<*>>,
  val serializers: Map<String, KSerializer<Any>>) {

  companion object {
    fun scan(vararg scanPackages: String): ChalaConfiguration {
      LOGGER.info("Scanning packages [${scanPackages.joinToString()}]:")
      val allClasses = scanPackages.flatMap { getClasses(it) }

      LOGGER.info("Scanning for classes with @${Entity::class.qualifiedName}:")
      val jpaClasses = allClasses.filterByAnnotation(Entity::class)

      LOGGER.info("Scanning for classes with @${Request::class.qualifiedName}:")
      val serializers = allClasses
        .filterByAnnotation(Request::class)
        .map { it.convertToChalaRequest() }
        .associate { it.qualifiedName!! to it.serializer() }

      return ChalaConfiguration(jpaClasses, serializers)
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
  val dataProperty = memberProperties.first { it.name == "data" }
  val dataPropertyType = dataProperty.returnType.classifier as KClass<*>

  if (!dataPropertyType.hasAnnotation<Serializable>())
    throw ChalaException("${dataPropertyType.qualifiedName} must be annotated with ${Serializable::class.qualifiedName}!")

  // this cannot fail if dataPropertyType is annotated with Serializable
  val companion = dataPropertyType.companionObjectInstance!!
  val serializerMethod = companion.javaClass.getMethod("serializer")
  return serializerMethod.invoke(companion) as KSerializer<Any>
}