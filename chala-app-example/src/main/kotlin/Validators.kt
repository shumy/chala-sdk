package net.chala.app.example

import net.chala.api.CheckAnnotation
import net.chala.api.CheckResult
import net.chala.api.ICheck
import net.chala.api.ICheckAnnotation

class NoAtSymbol : ICheck<String> {
  override fun check(value: String) =
    if (value.contains("@"))
      CheckResult.Error("String contains @")
    else CheckResult.Ok
}

@CheckAnnotation(CheckRange::class)
annotation class Range(val min: Int, val max: Int)

internal class CheckRange : ICheckAnnotation<Range, Int> {
  override fun check(annotation: Range, value: Int) =
    if (value < annotation.min || value > annotation.max)
      CheckResult.Error("Integer out of range [${annotation.min}, ${annotation.max}]")
    else CheckResult.Ok
}