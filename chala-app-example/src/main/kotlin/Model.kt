package net.chala.app.example

import net.chala.ChalaRecord
import net.chala.ChalaRepository
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class Student: ChalaRecord() {
  companion object : ChalaRepository<Student>()

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null

  lateinit var firstName: String

  lateinit var lastName: String

  lateinit var email: String
}