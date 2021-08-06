package net.chala.conf

import net.chala.ChalaConfigException
import net.chala.api.Check
import net.chala.api.CheckAnnotation
import net.chala.api.ICheck
import net.chala.api.ICheckAnnotation
import net.chala.utils.defaultConstructor
import net.chala.utils.getInterface
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

@Suppress("UNCHECKED_CAST")
internal fun KClass<*>.getObjectSpec(): ObjectSpec {
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
        throw ChalaConfigException("Checker ${ann.value.qualifiedName} (type=$checkerType) has incompatible type for field $className.$name ($returnType)!")

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
          throw ChalaConfigException("Checker ${it.value.qualifiedName} (type=$annotationType) has incompatible annotation for ${ann.annotationClass.qualifiedName}!")

        val checkerType = iChecker.arguments[1].type
        if (returnType != checkerType)
          throw ChalaConfigException("Checker ${it.value.qualifiedName} (type=$checkerType) has incompatible type for field $className.$name ($returnType)!")

        it.value.defaultConstructor().call() as ICheckAnnotation<Annotation, Any>
      }

      Pair(ann, instances)
    }
