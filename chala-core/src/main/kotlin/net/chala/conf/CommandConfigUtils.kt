package net.chala.conf

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import net.chala.ChalaConfigException
import net.chala.api.ICommand
import net.chala.api.Command
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.*

internal fun KClass<ICommand>.mapToCommandInfo(): CommandInfo {
  val command: Command = findAnnotation()!!
  val dataType = commandDataField().second
  val constructor = commandConstructor()
  val clazz = constructor.returnType.classifier as KClass<*>
  val classRef = clazz.qualifiedName!!
  if (!command.value.startsWith("/"))
    throw ChalaConfigException("@Command value for $classRef must start with /")

  return CommandInfo(command.value, command.document, classRef, dataType, constructor, commandSerializer())
}

@Suppress("UNCHECKED_CAST")
internal fun KClass<*>.convertToChalaCommand(): KClass<ICommand> {
  if (!isSubclassOf(ICommand::class))
    throw ChalaConfigException("$qualifiedName needs to implement ${ICommand::class.qualifiedName}!")

  return this as KClass<ICommand>
}

@Suppress("UNCHECKED_CAST")
private fun KClass<ICommand>.commandSerializer(): KSerializer<Any> {
  val dataField = commandDataField()
  if (!dataField.second.hasAnnotation<Serializable>())
    throw ChalaConfigException("${dataField.second.qualifiedName} must be annotated with ${Serializable::class.qualifiedName}!")

  // this cannot fail if dataPropertyType is annotated with Serializable
  val companion = dataField.second.companionObjectInstance!!
  val serializerMethod = companion.javaClass.getMethod("serializer")
  return serializerMethod.invoke(companion) as KSerializer<Any>
}

private fun KClass<ICommand>.commandConstructor(): KFunction<ICommand> {
  val dataField = commandDataField()

  val defaultConstructor = constructors.first { it.name == "<init>" }
  if (defaultConstructor.parameters.size != 1 && defaultConstructor.parameters[0].type != dataField.first)
    throw ChalaConfigException("$qualifiedName must have a default constructor (data: ${dataField.second.qualifiedName})!")

  return defaultConstructor
}

private fun KClass<ICommand>.commandDataField(): Pair<KType, KClass<*>> {
  val dataType = memberProperties.first { it.name == "data" }
  val dataClass = dataType.returnType.classifier as KClass<*>
  return Pair(dataType.returnType, dataClass)
}