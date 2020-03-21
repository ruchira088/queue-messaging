package com.ruchij

import java.util.concurrent.Executors

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import com.ruchij.config.KafkaConfiguration
import com.ruchij.kafka.consumer.Fs2KafkaConsumer
import com.ruchij.messaging.Topic
import com.ruchij.types.FunctionKTypes.fromEitherThrowable
import pureconfig.ConfigSource

import scala.concurrent.ExecutionContext

object App extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      kafkaConfiguration <- KafkaConfiguration.load[IO](configObjectSource)

      ioThreadPool <- IO.delay(ExecutionContext.fromExecutor(Executors.newCachedThreadPool()))

      fs2KafkaConsumer = new Fs2KafkaConsumer[IO](kafkaConfiguration, Blocker.liftExecutionContext(ioThreadPool))

      _ <-
        fs2KafkaConsumer.subscribe(Topic.NewUser)
          .evalMap {
            case (user, commit) =>
              IO.delay(println(s"FS2 Kafka: $user")).productR(commit)
          }
          .compile
          .drain

    } yield ExitCode.Success
}
