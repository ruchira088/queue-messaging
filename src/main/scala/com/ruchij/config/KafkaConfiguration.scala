package com.ruchij.config

import cats.effect.Sync
import cats.~>
import com.ruchij.messaging.Topic
import pureconfig.ConfigObjectSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

case class KafkaConfiguration(
  bootstrapServers: String,
  schemaRegistryUrl: String,
  topicPrefix: String,
  consumerGroupId: Option[String]
)

object KafkaConfiguration {
  def load[F[_]: Sync](
    configObjectSource: ConfigObjectSource
  )(implicit functionK: Either[Throwable, *] ~> F): F[KafkaConfiguration] =
    Sync[F].suspend {
      functionK {
        configObjectSource.at("kafka-configuration").load[KafkaConfiguration]
          .left.map(ConfigReaderException.apply)
      }
    }

  def topicName(topic: Topic[_], prefix: String): String = prefix + topic.name
}
