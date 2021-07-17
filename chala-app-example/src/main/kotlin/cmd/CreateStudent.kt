package net.chala.app.example.cmd

import kotlinx.serialization.Serializable
import net.chala.ChalaCommand
import net.chala.annotation.Command
import net.chala.app.example.Student

@Serializable
data class CreateStudent(val name: String, val index: Int)

@Command("/student/create", document = "student.yaml")
class CreateStudentCmd(override val data: CreateStudent) : ChalaCommand {
  override fun check() {
    println("      CreateStudentCmd.check: $data")
  }

  override fun validate() {
    println("      CreateStudentCmd.validate: $data")
    if (data.index % 3 == 2)
      throw RuntimeException("Simulated validation error!")
  }

  override fun commit() {
    println("      CreateStudentCmd.commit: $data")
    Student(null, data.name, data.name, "${data.name}@gmail.com").save()
  }
}