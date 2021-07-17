package net.chala.app.example

import net.chala.ChalaNode
import net.chala.conf.ChalaConfiguration
import net.chala.store.H2InMemoryConfig

fun main() {
  val conf = ChalaConfiguration.scan("net.chala.app.example")
  conf.chain = TestChain(2)
  conf.storeConf = H2InMemoryConfig(conf.jpaClasses)

  ChalaNode.setup(conf)
}