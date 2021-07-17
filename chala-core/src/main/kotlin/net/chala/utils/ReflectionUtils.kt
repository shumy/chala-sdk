package net.chala.utils

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

internal fun List<Class<*>>.filterClassByAnnotation(annotation: KClass<out Annotation>) =
  filter { it.isAnnotationPresent(annotation.java) }
    .map { it.kotlin }

internal fun Collection<KFunction<*>>.filterMethodByAnnotation(annotation: KClass<out Annotation>) =
  filter { it.javaMethod!!.isAnnotationPresent(annotation.java) }

internal fun KClass<*>.defaultConstructor(): KFunction<Any> =
  constructors.first { it.name == "<init>" }

internal fun getClasses(packageName: String): Sequence<Class<*>> {
  val classLoader = Thread.currentThread().contextClassLoader!!
  val path = packageName.replace('.', '/')
  val resources = classLoader.getResources(path)

  return resources.asSequence()
    .map { File(it.toURI()) }
    .flatMap { findClasses(it, packageName) }
}

internal fun findClasses(directory: File, packageName: String): List<Class<*>> {
  val classes = mutableListOf<Class<*>>()
  if (!directory.exists()) {
    return classes
  }

  val files = directory.listFiles()!!
  for (file in files) {
    if (file.isDirectory) {
      classes.addAll(findClasses(file, "$packageName.${file.name}"))
    } else if (file.name.endsWith(".class")) {
      classes.add(Class.forName("$packageName.${file.name.substring(0, file.name.length - 6)}"))
    }
  }

  return classes
}