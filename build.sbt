import Dependencies._

inThisBuild {
  Seq(
    organization := "com.ruchij",
    scalaVersion := SCALA_VERSION,
    version := "0.0.1",
    maintainer := "me@ruchij.com",
    buildInfoKeys := BuildInfoKey.ofN(name, organization, version, scalaVersion, sbtVersion),
    topLevelDirectory := None,
    scalacOptions ++= Seq("-Xlint", "-feature"),
    resolvers += "confluent" at "https://packages.confluent.io/maven/",
    addCompilerPlugin(kindProjector)
  )
}

lazy val root =
  (project in file("."))
    .enablePlugins(BuildInfoPlugin, JavaAppPackaging)
    .settings(
      name := "queue-messaging",
      libraryDependencies ++= rootDependencies ++ rootTestDependencies.map(_ % Test),
      buildInfoPackage := "com.eed3si9n.ruchij"
    )

lazy val kafkaProducer =
  (project in file("./kafka-producer"))
    .settings(name := "kafka-producer")
    .dependsOn(root)

lazy val akkaKafkaConsumer =
  (project in file("./akka-kafka-consumer"))
    .settings(name := "akka-kafka-consumer")
    .dependsOn(root)

lazy val fs2KafkaConsumer =
  (project in file("./fs2-kafka-consumer"))
    .settings(name := "fs2-kafka-consumer")
    .dependsOn(root)

lazy val rootDependencies =
  Seq(akkaStreamKafka, kafkaAvroSerializer, avro4sCore, catsEffect, fs2Core, pureconfig, javaFaker, kafka)

lazy val rootTestDependencies =
  Seq(scalaTest, pegdown)

addCommandAlias("testWithCoverage", "; coverage; test; coverageReport")
