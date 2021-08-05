package net.chala.service

import net.chala.api.ChalaRecord
import net.chala.api.ChalaRepository
import net.chala.utils.shaFingerprint
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity @Table(name = "chala_state")
internal class AppState (
  @Id @Column(nullable = false)
  val height: Long,

  @Column(nullable = false)
  val state: ByteArray,

  @Column(nullable = false)
  val txNumber: Int
): ChalaRecord() {
  companion object : ChalaRepository<AppState>()

  override fun toString(): String {
    val b64State = shaFingerprint(state)
    return "AppState($height, $b64State, $txNumber)"
  }
}