package net.chala

enum class RunContext {
  CLIENT, CHAIN, CHECK, VALIDATE, COMMIT
}

internal val chainContext = ThreadLocal<RunContext>()