/**
  * Created by cclient on 28/11/2017.
  */


import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010._
import org.apache.spark.{SparkConf, TaskContext}
import org.elasticsearch.spark.streaming._
import org.json4s.DefaultFormats
import org.json4s.jackson.parseJson

case class ES_Upsert(kw_index: String, kw_type: String, id: String, date_idate: String, date_udate: String)

case class ES_Doc(date_udate: String)

case class ES_UpsertDoc(upsert: ES_Upsert, doc: ES_Doc)

object UpsertDoc {
  def main(args: Array[String]) {
    if (args.length < 3) {
      System.err.println("Usage: Upsert <es_server> <kfk_server> <topic> <group> ")
      System.exit(1)
    }
    val Array(es, kfkserver, topic, group) = args
    val sparkConf = new SparkConf().setAppName("KafkaWordCount")
    val ssc = new StreamingContext(sparkConf, Seconds(20))

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> kfkserver,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> group,
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )
    val topics = Array(topic)
    val kStream = KafkaUtils.createDirectStream(ssc, LocationStrategies.PreferBrokers, Subscribe[String, String](topics, kafkaParams))
    val esDatas = kStream
      .map(_.value())
      .map(jsonstr => {
        implicit val formats = DefaultFormats
        val upsert = parseJson(jsonstr).extract[ES_Upsert]
        var doc = parseJson(jsonstr).extract[ES_Doc]
        ES_UpsertDoc(upsert, doc)
      })
      //upsert
      .saveToEs(Map[String, String](
      "es.resource" -> "{upsert.kw_index}/{upsert.kw_type}",
      "es.nodes" -> es,
      "es.input.json" -> "false",
      "es.nodes.discovery" -> "false",
      "es.update.doc" -> "true",
      "es.nodes.wan.only" -> "true",
      "es.write.operation" -> "upsert",
      "es.mapping.exclude" -> "upsert.kw_index,upsert.kw_type,upsert.id",
      "es.mapping.id" -> "upsert.id"
    ))
    kStream.foreachRDD { rdd =>
      val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      rdd.foreachPartition { iter =>
        val o: OffsetRange = offsetRanges(TaskContext.get.partitionId)
        println(s"${o.topic} ${o.partition} ${o.fromOffset} ${o.untilOffset}")
      }
      kStream.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
    }
    ssc.start()
    ssc.awaitTermination()
  }
}
