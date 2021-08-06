package net.chala.app.example

import net.chala.validation.CheckResult
import net.chala.validation.ICheck

class NoAtSymbol : ICheck<String> {
  override fun check(value: String) =
    if (value.contains("@"))
      CheckResult.Error("String contains @")
    else CheckResult.Ok
}