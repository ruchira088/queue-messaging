package com.ruchij.kafka.consumer

import java.time.Duration.ZERO
import java.util
import java.util.Properties

import cats.MonadError
import cats.effect.{Async, Blocker, Concurrent, ContextShift, Fiber, Sync, Timer}
import cats.implicits._
import com.ruchij.config.KafkaConfiguration
import com.ruchij.messaging.{Consumer, Topic}
import fs2.Stream
import io.confluent.kafka.serializers.{AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer
import org.apache.kafka.clients.consumer.{ConsumerConfig, OffsetAndMetadata}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.language.postfixOps

class Fs2KafkaConsumer[F[_]: Async: Timer: ContextShift: Concurrent](
  kafkaConfiguration: KafkaConfiguration,
  blocker: Blocker
) extends Consumer[Stream[F, *], F] {
  override type CommitOffset = Fiber[F, Map[TopicPartition, OffsetAndMetadata]]

  override def subscribe[A](topic: Topic[A]): Stream[F, (A, F[Fiber[F, Map[TopicPartition, OffsetAndMetadata]]])] =
    Stream
      .eval {
        Sync[F]
          .delay {
            new consumer.KafkaConsumer[String, AnyRef](new Properties() {
              put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfiguration.bootstrapServers)
              put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfiguration.consumerGroupId.getOrElse("fs2-consumer"))
              put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaConfiguration.schemaRegistryUrl)
              put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
              put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[KafkaAvroDeserializer].getName)
              put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false.toString)
              put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            })
          }
          .flatTap { kafkaConsumer =>
            Sync[F].delay {
              kafkaConsumer.subscribe {
                util.Collections.singletonList {
                  KafkaConfiguration.topicName(topic, kafkaConfiguration.topicPrefix)
                }
              }
            }
          }
      }
      .flatMap { kafkaConsumer =>
        Stream
          .awakeEvery[F](500 milliseconds)
          .evalMap { _ =>
            MonadError[F, Throwable].handleErrorWith {
              blocker.delay(kafkaConsumer.poll(ZERO))
            } { throwable =>
              Sync[F].delay(println(throwable)).flatMap(_ => MonadError[F, Throwable].raiseError(throwable))
            }
          }
          .flatMap { consumerRecords =>
            Stream.emits(consumerRecords.iterator().asScala.toSeq)
          }
          .map { consumerRecord =>
            (consumerRecord.value(), consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset())
          }
          .flatMap {
            case (genericRecord: GenericRecord, topicName, partition, offset) =>
              Stream.emit[F, (A, F[Fiber[F, Map[TopicPartition, OffsetAndMetadata]]])] {
                (topic.recordFormat.from(genericRecord), Concurrent[F].start {
                  Async[F].async[Map[TopicPartition, OffsetAndMetadata]] { callback =>
                    kafkaConsumer.commitAsync(
                      new util.HashMap[TopicPartition, OffsetAndMetadata]() {
                        put(new TopicPartition(topicName, partition), new OffsetAndMetadata(offset + 1))
                      },
                      (offsets: util.Map[TopicPartition, OffsetAndMetadata], exception: Exception) => {
                        Option(exception).fold(callback(Right(offsets.asScala.toMap))) { _ =>
                          callback(Left(exception))
                        }
                      }
                    )
                  }
                })
              }

            case _ => Stream.empty
          }
      }
}
