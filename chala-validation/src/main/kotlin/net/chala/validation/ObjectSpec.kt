package net.chala.validation

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

class ObjectSpec(
  val directChecks: Map<String, List<ICheck<Any>>>,
  val annotationChecks: Map<String, List<Pair<Annotation, List<ICheckAnnotation<Annotation, Any>>>>>
)

@Suppress("UNCHECKED_CAST")
fun KClass<*>.getObjectSpec(): ObjectSpec {
  // TODO: get instance checks

  val directChecks = memberProperties
    .filter { it.hasAnnotation<Check>() }
    .associate { it.name to it.getDirectChecks(qualifiedName!!) }

  val annotationChecks = memberProperties
    .filter { it.annotations.isNotEmpty() }
    .associate { it.name to it.getAnnotationChecks(qualifiedName!!) }

  return ObjectSpec(directChecks, annotationChecks)
}

@Suppress("UNCHECKED_CAST")
private fun KProperty1<*, *>.getDirectChecks(className: String) =
  annotations
    .filterIsInstance(Check::class.java)
    .map { ann ->
      val checkerType = ann.value.getInterface(ICheck::class).arguments[0].type
      if (returnType != checkerType)
        throw ObjectSpecException("Checker ${ann.value.qualifiedName} (type=$checkerType) has incompatible type for field $className.$name ($returnType)!")

      ann.value.defaultConstructor().call() as ICheck<Any>
    }

@Suppress("UNCHECKED_CAST")
private fun KProperty1<*, *>.getAnnotationChecks(className: String) =
  annotations
    .filter { it.annotationClass.hasAnnotation<CheckAnnotation>() }
    .map { ann ->
      val checkers = ann.annotationClass.annotations.filterIsInstance(CheckAnnotation::class.java)
      val instances = checkers.map {
        val iChecker = it.value.getInterface(ICheckAnnotation::class)

        val annotationType = iChecker.arguments[0].type?.classifier as KClass<*>
        if (ann.annotationClass != annotationType)
          throw ObjectSpecException("Checker ${it.value.qualifiedName} (type=$annotationType) has incompatible annotation for ${ann.annotationClass.qualifiedName}!")

        val checkerType = iChecker.arguments[1].type
        if (returnType != checkerType)
          throw ObjectSpecException("Checker ${it.value.qualifiedName} (type=$checkerType) has incompatible type for field $className.$name ($returnType)!")

        it.value.defaultConstructor().call() as ICheckAnnotation<Annotation, Any>
      }

      Pair(ann, instances)
    }

private fun KClass<*>.defaultConstructor(): KFunction<Any> =
  constructors.first { it.name == "<init>" }

@Suppress("UNCHECKED_CAST")
private fun KClass<*>.getInterface(interf: KClass<*>): KType =
  allSupertypes.first { (it.classifier as KClass<*>).qualifiedName == interf.qualifiedName }
