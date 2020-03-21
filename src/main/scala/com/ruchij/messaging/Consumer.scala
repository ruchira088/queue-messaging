package com.ruchij.messaging

trait Consumer[S[_], F[_]] {
  type CommitOffset

  def subscribe[A](topic: Topic[A]): S[(A, F[CommitOffset])]
}
