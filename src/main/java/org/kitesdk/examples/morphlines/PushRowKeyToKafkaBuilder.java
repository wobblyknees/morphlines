/*

* Copyright 2013 Cloudera Inc.

*

* Licensed under the Apache License, Version 2.0 (the "License");

* you may not use this file except in compliance with the License.

* You may obtain a copy of the License at

*

* http://www.apache.org/licenses/LICENSE-2.0

*

* Unless required by applicable law or agreed to in writing, software

* distributed under the License is distributed on an "AS IS" BASIS,

* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

* See the License for the specific language governing permissions and

* limitations under the License.

*/

package org.kitesdk.examples.morphlines;

 

import java.util.*;

 

import org.apache.hadoop.hbase.util.Bytes;

import org.kitesdk.morphline.api.Command;

import org.kitesdk.morphline.api.CommandBuilder;

import org.kitesdk.morphline.api.MorphlineContext;

import org.kitesdk.morphline.api.Record;

import org.kitesdk.morphline.base.AbstractCommand;

 

import com.typesafe.config.Config;

 

import org.apache.kafka.clients.producer.KafkaProducer;

import org.apache.kafka.clients.producer.Producer;

import org.apache.kafka.clients.producer.ProducerRecord;

 

 

 

/**

* Example custom morphline command that lowercases a string, and optionally reverses the order of

* the characters in the string.

*/

public final class PushRowkeyToKafkaBuilder implements CommandBuilder {

 

  @Override

  public Collection<String> getNames() {

    return Collections.singletonList("PushRowkeyToKafka");

  }

 

  @Override

  public Command build(Config config, Command parent, Command child, MorphlineContext context) {

    return new PushRowkeyToKafka (this, config, parent, child, context);

  }

 

  

  ///////////////////////////////////////////////////////////////////////////////

  // Nested classes:

  ///////////////////////////////////////////////////////////////////////////////

  private static final class PushRowkeyToKafka extends AbstractCommand {

   

    private final String topic;

    private final String bootStrapServers;

    private final String securityProtocol;

    private final String jaasPath;

 

 

    private Properties props;

 

    public PushRowkeyToKafka(CommandBuilder builder, Config config, Command parent, Command child, MorphlineContext context) {

      super(builder, config, parent, child, context);

 

      // capture the config from the morphline configuration

      this.topic = getConfigs().getString(config, "topic");

      this.bootStrapServers = getConfigs().getString(config, "bootStrapServers");

      this.securityProtocol = getConfigs().getString(config, "securityProtocol");

      this.jaasPath = getConfigs().getString(config, "jaasPath");

 

      validateArguments();

 

      // set the kafka properties

      props = new Properties();

      props.put("bootstrap.servers",bootStrapServers);

      props.put("security.protocol",securityProtocol);

      props.put("key.serializer",

              "org.apache.kafka.common.serialization.StringSerializer");

      props.put("value.serializer",

              "org.apache.kafka.common.serialization.StringSerializer");

    }

   

    @Override

    protected boolean doProcess(Record record) {

 

      // from the morhpline record, capture the rowkey

      org.apache.hadoop.hbase.client.Result result = (org.apache.hadoop.hbase.client.Result) record.getFirstValue("_attachment_body");

      byte[] rowKey = result.getRow();

 

      // pushback the row key field in case another morphline command needs it

      record.put("myRowKey", rowKey);

      String rowKeyStr = Bytes.toString(rowKey);

 

 

      // setup a kafka consumer

      // TODO: need to setup connection pooling to Kafka

      Producer<String, String> producer = new KafkaProducer

              <String, String>(props);

      producer.send(new ProducerRecord<String, String>(topic,

              rowKeyStr, rowKeyStr));

      System.out.println("msg sent successfully");

 

      // TODO: when we set up connection pooling we don't need to close the conection

      producer.close();

 

      // pass record to next command in chain:

      return super.doProcess(record);

    }

   

 

    @Override

    protected void doNotify(Record notification) {

      LOG.debug("myNotification: {}", notification);

      super.doNotify(notification);

    }

   

  }

 

}

 
