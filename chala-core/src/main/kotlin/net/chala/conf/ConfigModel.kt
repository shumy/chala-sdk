package net.chala.conf

import kotlinx.serialization.KSerializer
import net.chala.ChalaCommand
import net.chala.annotation.Command
import net.chala.annotation.Endpoint
import net.chala.annotation.Query
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class CommandInfo(
  val command: Command,
  val constructor: KFunction<ChalaCommand>,
  val serializer: KSerializer<Any>
)

class QueryInfo(
  val query: Query,
  val constructor: KFunction<Any>,
  val endpoints: List<EndpointInfo>
)

class EndpointInfo(
  val endpoint: Endpoint,
  val method: KFunction<*>
)