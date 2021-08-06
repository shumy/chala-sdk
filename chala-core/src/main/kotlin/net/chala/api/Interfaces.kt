package net.chala.api

interface ICommand {
  val data: Any

  fun validate(): Unit
  fun commit(): Unit
}