package net.chala.server

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.Javalin
import io.javalin.http.BadRequestResponse
import net.chala.ChalaException
import net.chala.ChalaNode
import net.chala.conf.ChalaConfiguration
import net.chala.conf.CommandInfo
import net.chala.conf.EndpointInfo
import net.chala.conf.QueryInfo
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

private val LOGGER = LoggerFactory.getLogger(ChalaServer::class.java)

private val mapper = jacksonObjectMapper()
  .registerModule(JavaTimeModule())
  .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  .setSerializationInclusion(JsonInclude.Include.NON_NULL)

internal class ChalaServer(config: ChalaConfiguration) {
  init {
    Javalin.create().start(7000).apply {
      setupErrorHandlers()
      LOGGER.info("Setup query/cmd endpoints")

      get("/status") { it.result("Chala REST server is UP") }
      LOGGER.info("GET /status")

      for (queryInfo in config.queries) {
        if (!queryInfo.query.value.startsWith("/"))
          throw ChalaException("@Query value for ${queryInfo.constructor.returnType} must start with /")

        for (endpointInfo in queryInfo.endpoints) {
          val endpointName = "${(queryInfo.constructor.returnType.classifier as KClass<*>).simpleName}.${endpointInfo.method.name}"
          if (endpointInfo.endpoint.value.isNotEmpty() && !endpointInfo.endpoint.value.startsWith("/"))
            throw ChalaException("@Endpoint value for $endpointName must start with /")

          val path = "/query" + queryInfo.query.value + endpointInfo.endpoint.value
          val params = endpointInfo.method.queryPathParams(path)

          setupGet(endpointName, queryInfo, endpointInfo, path, params)
        }
      }

      for (cmdInfo in config.commands) {
        val endpointName = (cmdInfo.value.constructor.returnType.classifier as KClass<*>).simpleName!!
        if (!cmdInfo.value.command.value.startsWith("/"))
          throw ChalaException("@Command value for $endpointName must start with /")

        val path = "/cmd" + cmdInfo.value.command.value
        setupPost(endpointName, cmdInfo.value, path)
      }
    }
  }
}

private fun Javalin.setupErrorHandlers() {
  exception(MissingKotlinParameterException::class.java) { ex, ctx ->
    val fieldType = (ex.parameter.type.classifier as KClass<*>).simpleName
    val error = RequestError(400, "Missing mandatory field (${ex.parameter.name}: $fieldType)")
    ctx.status(400)
    ctx.result(mapper.writeValueAsString(error))
  }

  exception(BadRequestResponse::class.java) { ex, ctx ->
    val error = RequestError(ex.status, ex.message)
    ctx.status(ex.status)
    ctx.result(mapper.writeValueAsString(error))
  }
}

private fun Javalin.setupGet(endpointName: String, queryInfo: QueryInfo, endpointInfo: EndpointInfo, path: String, params: List<KParameter>) {
  get(path) { ctx ->
    ctx.contentType("application/json")
    ChalaNode.node.startClientRequest()

    val queryService = queryInfo.constructor.call()
    val paramVals = params
      .map { ctx.pathParam(it.name!!, (it.type.classifier as KClass<*>).java).get() }
      .toTypedArray()

    val result = endpointInfo.method.call(queryService, *paramVals)
    if (result == null) {
      ctx.status(404)
      return@get
    }

    ctx.status(200)
    ctx.result(mapper.writeValueAsString(result))
  }

  val paramsSpec = params.joinToString(", ") { "${it.name}: ${(it.type.classifier as KClass<*>).simpleName}" }
  LOGGER.info("GET {} -> {}", path, "$endpointName($paramsSpec)")
}

private fun Javalin.setupPost(endpointName: String, cmdInfo: CommandInfo, path: String) {
  val dataType = (cmdInfo.constructor.parameters[0].type.classifier as KClass<*>).java
  post(path) { ctx ->
    ctx.contentType("application/json")
    ChalaNode.node.startClientRequest()

    val data = mapper.readValue(ctx.body(), dataType)
    // TODO: check javax.validation annotations?

    val command = cmdInfo.constructor.call(data)

    ChalaNode.submit(command)
    ctx.status(202)
  }

  LOGGER.info("POST {} -> {}", path, endpointName)
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
    throw ChalaException("Function $name is missing inputs for path $path")

  if (pathParams.size < parameters.size - 1)
    throw ChalaException("Function $name has inputs that are missing in path $path")

  return params
}