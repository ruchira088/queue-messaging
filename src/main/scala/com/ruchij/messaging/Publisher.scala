package com.ruchij.messaging

trait Publisher[F[_]] {
  type Result

  def publish[A: Topic](value: A): F[Result]
}
