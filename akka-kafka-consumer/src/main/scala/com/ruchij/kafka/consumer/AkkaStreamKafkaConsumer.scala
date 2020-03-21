package com.ruchij.kafka.consumer

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffset
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Source
import cats.effect.Sync
import com.ruchij.config.KafkaConfiguration
import com.ruchij.messaging.{Consumer, Topic}
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.StringDeserializer

import scala.jdk.CollectionConverters._

class AkkaStreamKafkaConsumer[F[_]: Sync](actorSystem: ActorSystem, kafkaConfiguration: KafkaConfiguration)
    extends Consumer[Source[*, Consumer.Control], F] {

  override type CommitOffset = CommittableOffset

  def subscribe[A](topic: Topic[A]): Source[(A, F[CommittableOffset]), Consumer.Control] =
    Consumer
      .committableSource(
        AkkaStreamKafkaConsumer.consumerSettings(actorSystem, kafkaConfiguration),
        Subscriptions.topics(KafkaConfiguration.topicName(topic, kafkaConfiguration.topicPrefix))
      )
      .map[(AnyRef, CommittableOffset)] { committableMessage =>
        (committableMessage.record.value(), committableMessage.committableOffset)
      }
      .mapConcat {
        case (genericRecord: GenericRecord, offset) =>
          List(topic.recordFormat.from(genericRecord) -> Sync[F].delay(offset))

        case _ => List.empty
      }
}

object AkkaStreamKafkaConsumer {
  def consumerSettings(
    actorSystem: ActorSystem,
    kafkaConfiguration: KafkaConfiguration
  ): ConsumerSettings[String, AnyRef] =
    ConsumerSettings(
      actorSystem,
      new StringDeserializer,
      new KafkaAvroDeserializer() {
        configure(
          Map(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> kafkaConfiguration.schemaRegistryUrl).asJava,
          false
        )
      }
    )
      .withBootstrapServers(kafkaConfiguration.bootstrapServers)
      .withGroupId(kafkaConfiguration.consumerGroupId.getOrElse("akka-stream-kafka-consumer"))
}
