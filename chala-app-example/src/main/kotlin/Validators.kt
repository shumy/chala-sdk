package net.chala.app.example

import net.chala.validation.CheckAnnotation
import net.chala.validation.CheckResult
import net.chala.validation.ICheck
import net.chala.validation.ICheckAnnotation

class NoAtSymbol : ICheck<String> {
  override fun check(value: String) =
    if (value.contains("@"))
      CheckResult.Error("String contains @")
    else CheckResult.Ok
}

@MustBeDocumented
@CheckAnnotation(CheckRange::class)
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Range(val min: Int, val max: Int)

internal class CheckRange : ICheckAnnotation<Range, Int> {
  override fun check(annotation: Range, value: Int) =
    if (value < annotation.min || value > annotation.max)
      CheckResult.Error("Integer out of range [${annotation.min}, ${annotation.max}]")
    else CheckResult.Ok
}