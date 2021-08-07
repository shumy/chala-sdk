package net.chala.validation

class ValidObject(
  @Check(NoAtSymbol::class)
  val text: String,

  @Check(NoAtSymbol::class)
  val nullableText: String? = null,

  @Check(LessThan10::class)
  val longNumber: Long,

  @Check(LessThan10::class)
  val nullableLongNumber: Long? = null,

  @Check(LessThan10::class)
  val number: Int,

  @Check(LessThan10::class)
  val nullableNumber: Int? = null,
)

class ExpectedStringObject(
  @Check(NoAtSymbol::class)
  val wrongText: Int
)

class InvalidCheckerObject(
  @Check(InvalidCheckerType::class)
  val number: Int
)

class InvalidNullCheckerObject(
  @Check(InvalidNullCheckerType::class)
  val number: Int
)

class InvalidAnnotationCheckerObject(
  @InvalidAnnotation
  val number: Int
)


