package net.chala.server

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.javalin.Javalin
import io.javalin.http.BadRequestResponse
import net.chala.ChalaNode
import net.chala.RunContext
import net.chala.chainContext
import net.chala.conf.ChalaConfiguration
import net.chala.conf.CommandInfo
import net.chala.conf.EndpointInfo
import net.chala.conf.QueryInfo
import net.chala.utils.JsonParser
import net.chala.validation.FieldConstraintException
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties

private val LOGGER = LoggerFactory.getLogger(ChalaServer::class.java)

internal class ChalaServer(config: ChalaConfiguration) {
  private val server = Javalin.create()
  private val cCommand: WsChannel

  init {
    server.apply {
      get("/") { it.result("Hello from ChalaServer!") }

      setupErrorHandlers()

      LOGGER.info("Mapping WS channels")
      cCommand = WsChannel(this, "/channel/cmd")

      LOGGER.info("Mapping query/cmd endpoints")
      for (queryInfo in config.queries)
        for (endpointInfo in queryInfo.endpoints)
          setupGet(queryInfo, endpointInfo, endpointInfo.path, endpointInfo.params)

      for (cmdInfo in config.commands)
        setupPost(cmdInfo.value, cmdInfo.value.path)

      start(7000)
    }
  }

  fun publish(committed: ChalaResponse) =
    cCommand.publish(JsonParser.json(committed))

  private fun Javalin.setupErrorHandlers() {
    exception(BadRequestResponse::class.java) { ex, ctx ->
      val error = BadInputResponse(ex.message)
      ctx.status(ex.status)
      ctx.result(JsonParser.json(error))
    }

    exception(InvalidFormatException::class.java) { ex, ctx ->
      val fieldName = ex.path[0]?.fieldName ?: "unknown-field"
      val message = ex.path[0]?.let { ref ->
        val prop = (ref.from as Class<*>).kotlin.memberProperties.first { it.name == fieldName }
        val fieldType = (prop.returnType.classifier as KClass<*>).simpleName
        "Invalid field type, expecting: $fieldType"
      }

      val error = BadFieldConstraintResponse(fieldName, message)
      ctx.status(400)
      ctx.result(JsonParser.json(error))
    }

    exception(MissingKotlinParameterException::class.java) { ex, ctx ->
      val error = BadFieldConstraintResponse(ex.parameter.name!!, "Missing mandatory field")
      ctx.status(400)
      ctx.result(JsonParser.json(error))
    }

    exception(FieldConstraintException::class.java) { ex, ctx ->
      val error = BadFieldConstraintResponse(ex.field, ex.message)
      ctx.status(400)
      ctx.result(JsonParser.json(error))
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
      ctx.result(JsonParser.json(result))
    }

    val paramsSpec = params.joinToString(", ") { "${it.name}: ${(it.type.classifier as KClass<*>).simpleName}" }
    LOGGER.info("GET  {} -> {}", path, "${endpointInfo.ref}($paramsSpec)")
  }

  private fun Javalin.setupPost(cmdInfo: CommandInfo, path: String) {
    val dataType = cmdInfo.constructor.parameters[0].type.classifier as KClass<*>
    post(path) { ctx ->
      ctx.contentType("application/json")
      chainContext.set(RunContext.CLIENT)

      val data = JsonParser.json(ctx.body(), dataType)
      val command = cmdInfo.constructor.call(data)

      val cmdId = ChalaNode.submit(command)
      val result = JsonParser.json(SubmittedResponse(cmdId))

      ctx.status(202)
      ctx.result(result)
      cCommand.publish(result)
    }

    LOGGER.info("POST {} -> {}", path, cmdInfo.ref)
  }
}