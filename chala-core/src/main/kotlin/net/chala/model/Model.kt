package net.chala.model

import kotlinx.serialization.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
internal class AppState (
  @Id
  val height: Long,

  @Column(nullable = false)
  val state: ByteArray
) {
  override fun toString(): String {
    val b64State = String(Base64.getEncoder().encode(state))
    return "AppState($height, $b64State)"
  }
}

@Serializable
open class DataPacket(val type: String, val data: ByteArray)