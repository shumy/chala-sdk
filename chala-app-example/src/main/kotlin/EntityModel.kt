package net.chala.app.example

import net.chala.ChalaRecord
import net.chala.ChalaRepository
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class Student(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  val firstName: String,
  val lastName: String,
  val email: String
): ChalaRecord() { companion object : ChalaRepository<Student>() }