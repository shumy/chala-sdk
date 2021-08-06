package net.chala.app.example.cmd

import kotlinx.serialization.Serializable
import net.chala.api.Command
import net.chala.api.ICommand
import net.chala.app.example.NoAtSymbol
import net.chala.app.example.Student
import net.chala.validation.Check
import net.chala.validation.Range

@Serializable
data class CreateStudent(
  @Check(NoAtSymbol::class)
  val name: String,

  @Range(min = 10, max = 20)
  val index: Int
)

@Command("/student/create", document = "student.yaml")
class CreateStudentCmd(override val data: CreateStudent) : ICommand {
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