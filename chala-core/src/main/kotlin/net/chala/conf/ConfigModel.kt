package net.chala.conf

import kotlinx.serialization.KSerializer
import net.chala.api.ICommand
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

class CommandInfo(
  val path: String,
  val document: String,
  val ref: String,
  val dataType: KClass<*>,
  val constructor: KFunction<ICommand>,
  val serializer: KSerializer<Any>
)

class QueryInfo(
  val document: String,
  val constructor: KFunction<Any>,
  val endpoints: List<EndpointInfo>
)

class EndpointInfo(
  val path: String,
  val ref: String,
  val method: KFunction<*>,
  val params: List<KParameter>
)