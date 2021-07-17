package net.chala.server

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.Javalin
import io.javalin.http.BadRequestResponse
import net.chala.ChalaNode
import net.chala.RunContext
import net.chala.chainContext
import net.chala.conf.ChalaConfiguration
import net.chala.conf.CommandInfo
import net.chala.conf.EndpointInfo
import net.chala.conf.QueryInfo
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

private val LOGGER = LoggerFactory.getLogger(ChalaServer::class.java)

private val mapper = jacksonObjectMapper()
  .registerModule(JavaTimeModule())
  .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  .setSerializationInclusion(JsonInclude.Include.NON_NULL)

internal class ChalaServer(config: ChalaConfiguration) {
  private val server = Javalin.create()

  init {
    server.apply {
      get("/") { it.result("Hello from ChalaServer!") }

      setupErrorHandlers()
      LOGGER.info("Mapping query/cmd endpoints")

      for (queryInfo in config.queries)
        for (endpointInfo in queryInfo.endpoints)
          setupGet(queryInfo, endpointInfo, "/query" + endpointInfo.path, endpointInfo.params)

      for (cmdInfo in config.commands)
        setupPost(cmdInfo.value, "/cmd" + cmdInfo.value.path)

      start(7000)
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

private fun Javalin.setupGet(queryInfo: QueryInfo, endpointInfo: EndpointInfo, path: String, params: List<KParameter>) {
  get(path) { ctx ->
    ctx.contentType("application/json")
    chainContext.set(RunContext.CLIENT)

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
  LOGGER.info("GET  {} -> {}", path, "${endpointInfo.ref}($paramsSpec)")
}

private fun Javalin.setupPost(cmdInfo: CommandInfo, path: String) {
  val dataType = (cmdInfo.constructor.parameters[0].type.classifier as KClass<*>).java
  post(path) { ctx ->
    ctx.contentType("application/json")
    chainContext.set(RunContext.CLIENT)

    val data = mapper.readValue(ctx.body(), dataType)
    val command = cmdInfo.constructor.call(data)

    ChalaNode.submit(command)
    ctx.status(202)
  }

  LOGGER.info("POST {} -> {}", path, cmdInfo.ref)
}