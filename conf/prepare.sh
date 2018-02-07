# create the hbase table
$ hbase shell
hbase shell> create 'test_table', {NAME => 'testcolumnfamily', REPLICATION_SCOPE => 1}

# create the dummy collection
$ solrctl instancedir --generate $HOME/hbase_collection_config
## Edit $HOME/hbase_collection_config/conf/schema.xml as needed ##
## If you are using Sentry for authorization, copy solrconfig.xml.secure to solrconfig.xml as follows: ##
## cp $HOME/hbase_collection_config/conf/solrconfig.xml.secure $HOME/hbase_collection_config/conf/solrconfig.xml ##
$ solrctl instancedir --create hbase_collection_config $HOME/hbase_collection_config
$ solrctl collection --create hbase_collection -s <numShards> -c hbase_collection_config

# add the indexer to lily
hbase-indexer add-indexer --name myIndexer --indexer-conf $HOME/morphline-hbase-mapper.xml --connection-param solr.zk=mstr1.feetytoes.com:2181/solr --connection-param solr.collection=hbase_collection --zookeeper mstr1:2181 --jaas jaas.conf
#hbase-indexer  list-indexers --zookeeper mstr1.feetytoes.com:2181
#hbase-indexer delete-indexer --name myIndexer --zookeeper mstr1.feetytoes.com:2181
