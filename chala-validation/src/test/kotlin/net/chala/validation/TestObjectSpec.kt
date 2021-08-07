package net.chala.validation

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class TestObjectSpec {
  @get:Rule
  val exceptionRule = ExpectedException.none()

  @Test fun `valid object-spec`() {
    ValidObject::class.getObjectSpec()
  }

  @Test fun `incompatible object-spec for string type`() {
    exceptionRule.expect(ObjectSpecException::class.java)
    exceptionRule.expectMessage("Validator net.chala.validation.NoAtSymbol (String) has incompatible type for field net.chala.validation.ExpectedStringObject.wrongText (Int)!")
    ExpectedStringObject::class.getObjectSpec()
  }

  @Test fun `invalid checker type`() {
    exceptionRule.expect(ObjectSpecException::class.java)
    exceptionRule.expectMessage("Validator net.chala.validation.InvalidCheckerType cannot use the type (Int), use (Long) instead!")
    InvalidCheckerObject::class.getObjectSpec()
  }

  @Test fun `invalid null checker type`() {
    exceptionRule.expect(ObjectSpecException::class.java)
    exceptionRule.expectMessage("Validator net.chala.validation.InvalidNullCheckerType (Long) cannot be marked as nullable!")
    InvalidNullCheckerObject::class.getObjectSpec()
  }

  @Test fun `invalid checker annotation`() {
    exceptionRule.expect(ObjectSpecException::class.java)
    exceptionRule.expectMessage("Validator net.chala.validation.CheckInvalidAnnotation (@Range) has incompatible annotation, requires @InvalidAnnotation!")
    InvalidAnnotationCheckerObject::class.getObjectSpec()
  }
}