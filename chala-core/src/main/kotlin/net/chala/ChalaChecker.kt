package net.chala

import net.chala.api.CheckResult
import net.chala.api.ICheck
import net.chala.api.ICheckAnnotation
import net.chala.conf.ObjectSpec
import kotlin.reflect.full.memberProperties

internal object ChalaChecker {
  fun check(objSpec: ObjectSpec, instance: Any) {
    val klass = instance.javaClass.kotlin

    // TODO: check object checker!
    //klass.annotations.filterIsInstance()

    for (field in klass.memberProperties) {
      val value = field.get(instance)
      if (value != null) {
        objSpec.directChecks[field.name]?.forEach { it.okOrThrow(field.name, value) }
        objSpec.annotationChecks[field.name]?.forEach { it.okOrThrow(field.name, value) }
      }
    }
  }
}

internal fun ICheck<Any>.okOrThrow(field: String, value: Any) {
  val result = check(value)
  if (result is CheckResult.Error)
    throw FieldConstraintException(field, result.msg)
}

internal fun Pair<Annotation, List<ICheckAnnotation<Annotation, Any>>>.okOrThrow(field: String, value: Any) =
  second.forEach {
    val result = it.check(first, value)
    if (result is CheckResult.Error)
      throw FieldConstraintException(field, result.msg)
  }

