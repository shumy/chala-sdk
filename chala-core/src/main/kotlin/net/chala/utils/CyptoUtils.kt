package net.chala.utils

import java.security.MessageDigest
import java.util.*

internal fun shaDigest(data: ByteArray): ByteArray {
  val digester = MessageDigest.getInstance("SHA-256")
  return digester.digest(data)
}

internal fun shaFingerprint(data: ByteArray): String {
  val txFingerprint = shaDigest(data)
  return String(Base64.getEncoder().encode(txFingerprint))
}