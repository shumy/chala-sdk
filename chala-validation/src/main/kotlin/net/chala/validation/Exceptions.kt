package net.chala.validation

import java.lang.RuntimeException

data class ObjectSpecException(override val message: String) : RuntimeException()

data class FieldConstraintException(val field: String, override val message: String) : RuntimeException()