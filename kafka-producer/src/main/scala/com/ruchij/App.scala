package com.ruchij

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import com.ruchij.config.KafkaConfiguration
import com.ruchij.kafka.producer.KafkaProducerImpl
import com.ruchij.model.User
import com.ruchij.random.RandomGenerator
import com.ruchij.types.FunctionKTypes.fromEitherThrowable
import fs2.Stream
import pureconfig.ConfigSource

import scala.concurrent.duration._
import scala.language.postfixOps

object App extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      kafkaConfiguration <- KafkaConfiguration.load[IO](configObjectSource)

      kafkaProducer = new KafkaProducerImpl[IO](kafkaConfiguration)

      _ <- RandomGenerator[IO, User].stream
        .zipLeft(Stream.awakeEvery[IO](100 milliseconds))
        .evalMap { user => IO.delay(println(s"Publishing: $user")).as(user) }
        .evalMap { user =>
          kafkaProducer.publish(user)
        }
        .compile
        .drain

    } yield ExitCode.Success
}
