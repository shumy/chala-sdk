package net.chala.validation

import kotlin.reflect.full.memberProperties

fun ObjectSpec.check(instance: Any) {
  val klass = instance.javaClass.kotlin

  // TODO: check object checker!
  //klass.annotations.filterIsInstance()

  for (field in klass.memberProperties) {
    val value = field.get(instance)
    if (value != null) {
      directChecks[field.name]?.forEach { it.okOrThrow(field.name, value) }
      annotationChecks[field.name]?.forEach { it.okOrThrow(field.name, value) }
    }
  }
}

private fun ICheck<Any>.okOrThrow(field: String, value: Any) {
  val result = check(value)
  if (result is CheckResult.Error)
    throw FieldConstraintException(field, result.msg)
}

private fun Pair<Annotation, List<ICheckAnnotation<Annotation, Any>>>.okOrThrow(field: String, value: Any) =
  second.forEach {
    val result = it.check(first, value)
    if (result is CheckResult.Error)
      throw FieldConstraintException(field, result.msg)
  }