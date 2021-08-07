package net.chala.validation

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

val objSpec = ValidObject::class.getObjectSpec()

class TestObjectValidation {
  @get:Rule
  val exceptionRule = ExpectedException.none()

  @Test
  fun `valid object`() {
    val obj = ValidObject(
      text = "normal string",
      longNumber = 9L,
      number = 8,
      nullableNumber = 7
    )

    objSpec.check(obj)
  }

  @Test
  fun `invalid object - "Text with @"`() {
    exceptionRule.expect(FieldConstraintException::class.java)
    exceptionRule.expectMessage("Text contains @")
    val obj = ValidObject(
      text = "normal @ string",
      longNumber = 9L,
      number = 8
    )

    objSpec.check(obj)
  }

  @Test
  fun `invalid object - "Number should be less than 10"`() {
    exceptionRule.expect(FieldConstraintException::class.java)
    exceptionRule.expectMessage("Number should be less than 10")
    val obj = ValidObject(
      text = "normal string",
      longNumber = 10L,
      number = 8
    )

    objSpec.check(obj)
  }

  @Test
  fun `invalid object (for nullable field)- "Number should be less than 10"`() {
    exceptionRule.expect(FieldConstraintException::class.java)
    exceptionRule.expectMessage("Number should be less than 10")
    val obj = ValidObject(
      text = "normal string",
      longNumber = 9L,
      number = 8,
      nullableNumber = 11
    )

    objSpec.check(obj)
  }
}