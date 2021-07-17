package net.chala.app.example.query

import net.chala.api.Endpoint
import net.chala.api.Query
import net.chala.app.example.Student

@Query("/students", document = "students.yaml")
class StudentQuery {
  @Endpoint
  fun list() = Student.findAll()

  @Endpoint("/:id")
  fun findById(id: Long) = Student.findById(id)
}