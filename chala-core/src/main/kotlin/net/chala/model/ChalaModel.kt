package net.chala.model

import kotlinx.serialization.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
internal class AppState (
  @Id
  val height: Long,

  @Column(nullable = false)
  val state: String
)

@Serializable
open class DataPacket(val type: String, val data: ByteArray)