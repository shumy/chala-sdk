package net.chala.validation

@MustBeDocumented
@Target(AnnotationTarget.PROPERTY)
@CheckAnnotation(CheckRange::class)
annotation class Range(val min: Int, val max: Int)

internal class CheckRange : ICheckAnnotation<Range, Int> {
  override fun check(annotation: Range, value: Int) =
    if (value < annotation.min || value > annotation.max)
      CheckResult.Error("Number out of range [${annotation.min}, ${annotation.max}]")
    else CheckResult.Ok
}