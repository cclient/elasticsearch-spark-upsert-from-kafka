
name := "elasticsearch-spark-upsert-from-kafka"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.2.0" % "provided"
libraryDependencies += "org.apache.spark" % "spark-streaming_2.11" % "2.2.0"
libraryDependencies += "org.apache.spark" % "spark-streaming-kafka-0-10_2.11" % "2.2.0"
libraryDependencies += "org.apache.kafka" % "kafka_2.11" % "1.0.0"

//libraryDependencies += "org.elasticsearch" % "elasticsearch-spark-20_2.11" % "6.0.0-alpha2"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"
libraryDependencies += "org.elasticsearch" % "elasticsearch-spark-20_2.11" % "7.0.0-alpha1-SNAPSHOT"
libraryDependencies += "org.apache.spark" % "spark-sql_2.11" % "2.1.0"