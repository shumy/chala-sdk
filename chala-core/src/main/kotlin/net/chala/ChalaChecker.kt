package net.chala

import net.chala.api.CheckResult
import net.chala.api.ICheck
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
        // direct check
        //println("Check ${field.name} - $value with ${objSpec.directChecks[field.name]}")
        objSpec.directChecks[field.name]?.forEach { it.okOrThrow(field.name, value) }

        // annotation check
      }
    }
  }
}

internal fun ICheck<Any>.okOrThrow(field: String, value: Any) {
  val result = check(value)
  if (result is CheckResult.Error)
    throw FieldConstraintException(field, result.msg)
}

