FROM gettyimages/spark:2.2.0-hadoop-2.7
ADD jars /usr/spark-2.2.0/jars
ADD target/scala-2.11/elasticsearch-spark-upsert-from-kafka_2.11-1.0.jar /usr/spark-2.2.0/
