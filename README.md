# Kite - Morphlines Examples


## Create HBase Table and Solr Collection

```
$ hbase shell
 
hbase shell> create 'sample_table', {NAME => 'cf1', REPLICATION_SCOPE => 1}
Create The Dummy Collection
```

```
# create the dummy collection
$ solrctl instancedir --generate $HOME/hbase_collection_config
## Edit $HOME/hbase_collection_config/conf/schema.xml as needed ##
## If you are using Sentry for authorization, copy solrconfig.xml.secure to solrconfig.xml as follows: ##
## cp $HOME/hbase_collection_config/conf/solrconfig.xml.secure $HOME/hbase_collection_config/conf/solrconfig.xml ##
$ solrctl instancedir --create hbase_collection_config $HOME/hbase_collection_config
$ solrctl collection --create hbase_collection -s 1 -c hbase_collection_config
```

## Create The Morphline-Hbase-Mapper.Xml File


The morphline-hbase-mapper.xml file tells the Lily indexer what hbase table will be listened to. It also points out the location to the morphlines.conf file.

Create this file as a text file on the server you have deployed the Lily Indexer Service.  It must be readable by the 'hbase' linux user.

```
<?xml version="1.0"?>
<indexer table="sample_table"
mapper="com.ngdata.hbaseindexer.morphline.MorphlineResultToSolrMapper">
 
<!-- The relative or absolute path on the local file system to the
morphline configuration file. -->
<!-- Use relative path "morphlines.conf" for morphlines managed by
Cloudera Manager -->
<param name="morphlineFile" value="morphlines.conf"/>
 
<!-- The optional morphlineId identifies a morphline if there are multiple
morphlines in morphlines.conf -->
<!-- <param name="morphlineId" value="morphline1"/> -->
 
</indexer>
 
```


## Create A Special Lily Indexer Jaas.Conf File


This particular version needs a Client entry for the 'hbase' principal, and a Kafka entry for the functional ID.

I create this on the Lily Indexer host  in /opt/cloudera/security/lilyjaas.conf

Its contents are


```
KafkaClient {
com.sun.security.auth.module.Krb5LoginModule required
useKeyTab=true
useTicketCache=false
renewTicket=false
storeKey=true
serviceName="kafka"
keyTab="<Path to keytab>
principal="<function ID>@<REALM>";
};
Client {
com.sun.security.auth.module.Krb5LoginModule required
useKeyTab=true
useTicketCache=false
keyTab="hbase.keytab"
principal="<hbase principal>@<REALM>";
};

```


## Create The Indexer Specifically For The HBase Table



```
hbase-indexer add-indexer --name myIndexer --indexer-conf $HOME/morphline-hbase-mapper.xml --connection-param solr.zk=<ZK host>:2181/solr --connection-param solr.collection=hbase_collection --zookeeper <ZK host>:2181 --jaas /opt/cloudera/security/jaas.conf
``` 


## Configure The Lily Indexer
Goto Key-Value Store Indexer->ConfigurationMorphlines

Replace all of its content with this:



```
morphlines : [
{
id : morphline
importCommands : ["org.kitesdk.**", "com.ngdata.**"]
 
commands : [ 
{
PushRowkeyToKafka {
topic: "myTopic" 
bootStrapServers: "<kafka server list>"
securityProtocol: "SASL_SSL"
jaasPath="<path to jaas.conf>" 
}
}
{
extractHBaseCells {
mappings : [
{
inputColumn : "cf1:data"
outputField : "data_s" 
type : string 
source : value
}
]
}
}
 
{
dropRecord {}
}
 
{ logDebug { format : "output record: {}", args : ["@{}"] } }
]
}
]

```

Modify the "Key-Value Store Indexer Service Environment Advanced Configuration Snippet (Safety Valve)" with this:


```
HBASE_INDEXER_CLASSPATH=/data1/kite-examples-morphlines-1.1.0.jar:/opt/cloudera/parcels/KAFKA/lib/kafka/libs/kafka-clients-0.11.0-kafka-3.0.0.jar
JAAS_FILE=../../../../../opt/cloudera/security/lilyjaas.conf
``` 


Note how we are adding the Kafka and kite morphline JAR to the INDEXER class path.



## Start The Key-Value Store Indexer Service



## Add/Update Records In Hbase
