package net.chala.conf

import net.chala.ChalaConfigException
import net.chala.annotation.Endpoint
import net.chala.annotation.Query
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

internal fun KClass<*>.mapToQueryInfo(): QueryInfo {
  val query: Query = findAnnotation()!!
  val constructor = queryConstructor()
  val clazz = constructor.returnType.classifier as KClass<*>
  val classRef = clazz.qualifiedName!!
  if (!query.value.startsWith("/"))
    throw ChalaConfigException("@Query value for $classRef must start with /")

  return QueryInfo(query.document, constructor, queryEndpoints(query.value, classRef))
}

private fun KClass<*>.queryConstructor(): KFunction<Any> {
  val defaultConstructor = constructors.first { it.name == "<init>" }
  if (defaultConstructor.parameters.isNotEmpty())
    throw ChalaConfigException("$qualifiedName must have an empty default constructor!")

  return defaultConstructor
}

private fun KClass<*>.queryEndpoints(queryPath: String, classRef: String): List<EndpointInfo> =
  memberFunctions
    .filterMethodByAnnotation(Endpoint::class)
    .map {
      val endpoint: Endpoint = it.findAnnotation()!!
      val methodRef = "$classRef.${it.name}"
      if (endpoint.value.isNotEmpty() && !endpoint.value.startsWith("/"))
        throw ChalaConfigException("@Endpoint value for $methodRef must start with /")

      val path = queryPath + endpoint.value
      val params = it.queryPathParams(path)
      EndpointInfo(path, methodRef, it, params)
    }

private fun KFunction<*>.queryPathParams(path: String): List<KParameter> {
  val pathParams = path
    .split("/")
    .filter { it.startsWith(":") }
    .map { it.substring(1) }
    .toSet()

  val params = parameters
    .drop(1)
    .filter { pathParams.contains(it.name) }

  if (pathParams.size > parameters.size - 1)
    throw ChalaConfigException("Function $name is missing inputs for path $path")

  if (pathParams.size < parameters.size - 1)
    throw ChalaConfigException("Function $name has inputs that are missing in path $path")

  return params
}