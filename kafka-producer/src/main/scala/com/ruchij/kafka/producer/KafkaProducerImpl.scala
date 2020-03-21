package com.ruchij.kafka.producer

import java.util.Properties

import cats.effect.Async
import com.ruchij.config.KafkaConfiguration
import com.ruchij.messaging.{Publisher, Topic}
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroSerializer}
import org.apache.kafka.clients.producer
import org.apache.kafka.clients.producer.{Callback, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

class KafkaProducerImpl[F[_]: Async](kafkaConfiguration: KafkaConfiguration) extends Publisher[F] {
  override type Result = RecordMetadata

  val kafkaProducer =
    new producer.KafkaProducer[String, AnyRef](new Properties() {
      put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfiguration.bootstrapServers)
      put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaConfiguration.schemaRegistryUrl)
      put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
      put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[KafkaAvroSerializer])
    })

  override def publish[A: Topic](value: A): F[RecordMetadata] =
    Async[F].async {
      callback =>
        kafkaProducer.send(
          new ProducerRecord(
            KafkaConfiguration.topicName(Topic[A], kafkaConfiguration.topicPrefix),
            Topic[A].recordFormat.to(value)
          ),
          new Callback {
            override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit =
              Option(exception).fold(callback(Right(metadata))) { throwable => callback(Left(throwable)) }
          }
        )
    }
}
