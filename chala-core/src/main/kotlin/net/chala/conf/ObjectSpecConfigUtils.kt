package net.chala.conf

import net.chala.ChalaConfigException
import net.chala.api.Check
import net.chala.api.ICheck
import net.chala.utils.defaultConstructor
import net.chala.utils.getInterface
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

@Suppress("UNCHECKED_CAST")
internal fun KClass<*>.getObjectSpec(): ObjectSpec {
  val directChecks = memberProperties
    .filter { prop -> prop.hasAnnotation<Check>() }
    .associate { prop ->
      prop.name to prop.annotations.filterIsInstance(Check::class.java).map { checker ->
        val checkerType = checker.value.getInterface(ICheck::class).arguments.first().type
        if (prop.returnType != checkerType)
          throw ChalaConfigException("Checker ${checker.value.qualifiedName} (type=$checkerType) has incompatible type for field ${qualifiedName}.${prop.name} (${prop.returnType})!")

        checker.value.defaultConstructor().call() as ICheck<Any>
      }
    }

  return ObjectSpec(directChecks)
}
