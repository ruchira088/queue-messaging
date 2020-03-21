import sbt._

object Dependencies
{
  val SCALA_VERSION = "2.13.1"

  lazy val akkaStreamKafka = "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.2"

  lazy val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.12.3"

  lazy val kafkaAvroSerializer = "io.confluent" % "kafka-avro-serializer" % "5.4.1"

  lazy val avro4sCore = "com.sksamuel.avro4s" %% "avro4s-core" % "3.0.9"

  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "2.1.2"

  lazy val fs2Core = "co.fs2" %% "fs2-core" % "2.2.2"

  lazy val kafka = "org.apache.kafka" %% "kafka" % "2.4.1"

  lazy val kindProjector = "org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full

  lazy val javaFaker = "com.github.javafaker" % "javafaker" % "1.0.2"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1"

  lazy val pegdown = "org.pegdown" % "pegdown" % "1.6.0"
}
