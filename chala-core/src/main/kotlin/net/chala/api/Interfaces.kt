package net.chala.api

sealed class CheckResult {
  object Ok : CheckResult()
  data class Error(val msg: String) : CheckResult()
}

interface ICheck<T> {
  fun check(value: T): CheckResult
}

interface ICheckAnnotation<A : Annotation, T> {
  fun check(annotation: A, value: T): CheckResult
}

interface ICommand {
  val data: Any

  fun validate(): Unit
  fun commit(): Unit
}