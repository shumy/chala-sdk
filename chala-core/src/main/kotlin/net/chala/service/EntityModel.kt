package net.chala.service

import net.chala.ChalaRecord
import net.chala.ChalaRepository
import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity @Table(name = "chala_state")
internal class AppState (
  @Id
  val height: Long,

  @Column(nullable = false)
  val state: ByteArray,
): ChalaRecord() {
  companion object : ChalaRepository<AppState>()

  override fun toString(): String {
    val b64State = String(Base64.getEncoder().encode(state))
    return "AppState($height, $b64State)"
  }
}