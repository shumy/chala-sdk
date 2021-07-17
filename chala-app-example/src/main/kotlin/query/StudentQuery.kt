package net.chala.app.example.query

import net.chala.annotation.Endpoint
import net.chala.annotation.Query
import net.chala.app.example.Student

@Query("/student", document = "student.yaml")
class StudentQuery {
  @Endpoint
  fun list() = Student.findAll()

  @Endpoint("/:id")
  fun findById(id: Long) = Student.findById(id)
}