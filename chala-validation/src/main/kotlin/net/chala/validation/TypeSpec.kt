package net.chala.validation

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

private val invalidCheckerTypes = setOf(Int::class, Float::class)

private val primitiveConversion = mapOf(
  Int::class to Long::class,
  Float::class to Double::class
)

internal fun KProperty1<*, *>.isCompatibleWith(annotationName: String, checkerName: String, checkerType: KType) {
  val checkerKlass = checkerType.classifier as KClass<*>
  val originalFieldKlass = returnType.classifier as KClass<*>
  val fieldKlass = primitiveConversion[originalFieldKlass] ?: originalFieldKlass

  if(checkerType.isMarkedNullable)
    throw ObjectSpecException("Validator $annotationName (${checkerKlass.simpleName}) cannot be marked as nullable!")

  // TODO: checkerType cannot be a MutableCollection

  if(invalidCheckerTypes.contains(checkerKlass))
    throw ObjectSpecException("Validator $annotationName cannot use the type (${checkerKlass.simpleName}), use (${primitiveConversion[checkerKlass]?.simpleName}) instead!")

  if (!fieldKlass.isSubclassOf(checkerKlass)) {
    throw ObjectSpecException("Validator $annotationName (${checkerKlass.simpleName}) has incompatible type for field $checkerName.$name (${originalFieldKlass.simpleName})!")
  }
}