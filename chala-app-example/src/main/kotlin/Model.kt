package net.chala.app.example

//@Entity
class Student {

  //@Id
  //@GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null

  lateinit var firstName: String

  lateinit var lastName: String

  lateinit var email: String
}