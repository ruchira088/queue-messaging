package com.ruchij.messaging

import com.ruchij.model.User
import com.sksamuel.avro4s.RecordFormat

sealed trait Topic[A] {
  val recordFormat: RecordFormat[A]

  val name: String
}

object Topic {
  def apply[A](implicit topic: Topic[A]): Topic[A] = topic

  implicit case object NewUser extends Topic[User] {
    override val recordFormat: RecordFormat[User] = RecordFormat[User]

    override val name: String = "NewUser"
  }
}
