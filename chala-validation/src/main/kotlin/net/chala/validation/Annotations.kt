package net.chala.validation

import kotlin.reflect.KClass

@Repeatable
@MustBeDocumented
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Check(val value: KClass<out ICheck<*>>)

@Repeatable
@MustBeDocumented
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CheckAnnotation(val value: KClass<out ICheckAnnotation<*, *>>)