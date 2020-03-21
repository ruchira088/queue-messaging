package com.ruchij.random

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import com.github.javafaker.Faker
import com.ruchij.model.User
import fs2.Stream

import scala.util.Random

trait RandomGenerator[F[_], +A] {
  def value[B >: A]: F[B]

  val stream: Stream[F, A] = Stream.repeatEval(value[A])
}

object RandomGenerator {
  def apply[F[_], A](implicit randomGenerator: RandomGenerator[F, A]): RandomGenerator[F, A] = randomGenerator

  val faker: Faker = Faker.instance()

  import faker._

  implicit def userGenerator[F[_]: Sync]: RandomGenerator[F, User] =
    new RandomGenerator[F, User] {
      override def value[B >: User]: F[B] =
        option(range(0, 100)).flatMap(age => Sync[F].delay(User(name().firstName(), name().lastName(), age)))

      override val stream: Stream[F, User] =
        numbers[F](0).evalMap { age => Sync[F].delay(User(name().firstName(), name().lastName(), Some(age))) }
    }

  def option[F[_]: Sync, A](value: F[A]): F[Option[A]] =
    Sync[F].delay(Random.nextBoolean())
      .flatMap { isSome => if (isSome) value.map(Some.apply) else Applicative[F].pure(None) }

  def range[F[_]: Sync](min: Int, max: Int): F[Int] =
    Sync[F].delay(Random.nextInt(max - min)).map(_ + min)

  def numbers[F[_]](n: Int): Stream[F, Int] = Stream[F, Int](n) ++ numbers(n + 1)
}