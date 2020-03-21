package com.ruchij

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import cats.effect.{ExitCode, IO, IOApp}
import com.eed3si9n.ruchij.BuildInfo
import com.ruchij.config.KafkaConfiguration
import com.ruchij.kafka.consumer.AkkaStreamKafkaConsumer
import com.ruchij.messaging.Topic
import com.ruchij.types.FunctionKTypes.fromEitherThrowable
import pureconfig.ConfigSource

object App extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      kafkaConfiguration <- KafkaConfiguration.load[IO](configObjectSource)

      actorSystem = ActorSystem(BuildInfo.name)
      actorMaterializer = ActorMaterializer()(actorSystem)

      akkaStreamKafkaConsumer = new AkkaStreamKafkaConsumer[IO](actorSystem, kafkaConfiguration)

      _ <- IO.fromFuture {
        IO.delay {
          akkaStreamKafkaConsumer
            .subscribe(Topic.NewUser)
            .runWith(Sink.foreach { case (user, _) => println(s"Akka Stream Kafka: $user") })(actorMaterializer)
        }
      }

    } yield ExitCode.Success
}
