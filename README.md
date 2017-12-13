# elasticsearch-hadoop upsert doc demo,it isn't graceful but can work
```$xslt
{
      "upsert": {},
      "doc": {...}
}
```

## first sorry for my poor English


#### for this demo the elasticsearch-hadoop version is 7.0.0-alpha1-SNAPSHOT

## elasticsearch-hadoop

### rebuild elasticsearch-hadoop or direct install jars/elasticsearch-spark-20_2.11-7.0.0-alpha1-SNAPSHOT.jar

#### rebuild and install to maven by self
git clone https://github.com/cclient/elasticsearch-hadoop 

cd elasticsearch-hadoop

./gradlew assemble

./gradlew install

./gradlew writePom

#### direct install builded jar

mvn install:install-file -DgroupId=org.elasticsearch -DartifactId=elasticsearch-spark-20_2.11 -Dversion=7.0.0-alpha1-SNAPSHOT -Dpackaging=jar -Dfile=jars/elasticsearch-spark-20_2.11-7.0.0-alpha1-SNAPSHOT.jar

### only a small modify and easy to do by self

see [commit](https://github.com/cclient/elasticsearch-hadoop/commit/27071fe4b842011a06126c15b8c0e22f75cdc42f) 

## spark saveES upsert 

### build.sbt

```old
libraryDependencies += "org.elasticsearch" % "elasticsearch-spark-20_2.11" % "6.0.0-alpha2"
```

->

```new
resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies += "org.elasticsearch" % "elasticsearch-spark-20_2.11" % "7.0.0-alpha1-SNAPSHOT"

libraryDependencies += "org.apache.spark" % "spark-sql_2.11" % "2.1.0"
```

resolvers Local Maven Repository to use local 7.0.0-alpha1-SNAPSHOT


import spark-sql_2.11 because build/install 7.0.0-alpha1-SNAPSHOT doesn't install the dependences

#### upsert like UpsertDoc.scala

```scala 
case class ES_Upsert(kw_index: String,kw_type: String,id: String,date_idate: String,date_udate:String)

case class ES_Doc(date_udate: String)

case class ES_UpsertDoc(upsert:ES_Upsert,doc:ES_Doc)

def upsert(esDatas: DStream[ES_UpsertDoc]) = {
    esDatas.saveToEs(Map[String, String](
      "es.resource" -> "{upsert.kw_index}/{upsert.kw_type}",
      "es.nodes" -> ES,
      "es.input.json" -> "false",
      "es.nodes.discovery" -> "false",
      "es.update.doc" -> "true",
      "es.nodes.wan.only" -> "true",
      "es.write.operation" -> "upsert",
      "es.mapping.exclude" -> "upsert.kw_index,upsert.kw_type,upsert.id",
      "es.mapping.id" -> "upsert.id"
    ))
  }
```  


different part

```diff
"es.resource" -> "{upsert.kw_index}/{upsert.kw_type}",
"es.update.doc"->"true",
"es.mapping.exclude" -> "upsert.kw_index,upsert.kw_type,upsert.id",
"es.mapping.id" -> "upsert.id"
```


## run (env is gettyimages/spark:2.2.0-hadoop-2.7)

### 1 package

sbt package

### 2 build docker image
 
docker build -t my_spark ./


### 3 submit spark job

docker run -d  --name my_spark my_spark

docker exec -it my_spark bash

spark-submit --class UpsertDoc elasticsearch-spark-upsert-from-kafka_2.11-1.0.jar elasticsearch:9200 kafka:9092 test_topic test_group
