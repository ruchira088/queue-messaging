package com.ruchij.types

import cats.{Applicative, MonadError, ~>}

object FunctionKTypes {
  implicit def fromEitherThrowable[L, F[_]: MonadError[*[_], L]]: Either[L, *] ~> F =
    new ~>[Either[L, *], F] {
      override def apply[A](either: Either[L, A]): F[A] =
        either.fold[F[A]](MonadError[F, L].raiseError, Applicative[F].pure)
    }
}
