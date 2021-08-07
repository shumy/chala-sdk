package net.chala.validation

class NoAtSymbol : ICheck<String> {
  override fun check(value: String) =
    if (value.contains("@"))
      CheckResult.Error("Text contains @")
    else CheckResult.Ok
}


class LessThan10 : ICheck<Long> {
  override fun check(value: Long) =
    if (value > 9)
      CheckResult.Error("Number should be less than 10")
    else CheckResult.Ok
}

class InvalidNullCheckerType : ICheck<Long?> {
  override fun check(value: Long?) = CheckResult.Ok
}

class InvalidCheckerType : ICheck<Int> {
  override fun check(value: Int) = CheckResult.Ok
}

@Target(AnnotationTarget.PROPERTY)
@CheckAnnotation(CheckInvalidAnnotation::class)
annotation class InvalidAnnotation

internal class CheckInvalidAnnotation : ICheckAnnotation<Range, Int> {
  override fun check(annotation: Range, value: Int) = CheckResult.Ok
}