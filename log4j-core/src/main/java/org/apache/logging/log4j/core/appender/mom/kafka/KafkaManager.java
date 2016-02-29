/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.core.appender.mom.kafka;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.Log4jThread;

public class KafkaManager extends AbstractManager {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_TIMEOUT_MILLIS = "30000";

    /**
     * package-private access for testing.
     */
    static KafkaProducerFactory producerFactory = new DefaultKafkaProducerFactory();

    private final Properties config = new Properties();
    private transient Producer<byte[], byte[]> producer = null;
    private final int timeoutMillis;

    private final String topic;

    public KafkaManager(final String name, final String topic, final Property[] properties) {
        super(name);
        this.topic = topic;
        config.setProperty("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        config.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        config.setProperty("batch.size", "0");
        for (final Property property : properties) {
            config.setProperty(property.getName(), property.getValue());
        }
        this.timeoutMillis = Integer.parseInt(config.getProperty("timeout.ms", DEFAULT_TIMEOUT_MILLIS));
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.producer = null;
    }

    @Override
    public void releaseSub() {
        if (producer != null) {
            // This thread is a workaround for this Kafka issue: https://issues.apache.org/jira/browse/KAFKA-1660
            final Thread closeThread = new Log4jThread(new Runnable() {
                @Override
                public void run() {
                    producer.close();
                }
            });
            closeThread.setName("KafkaManager-CloseThread");
            closeThread.setDaemon(true); // avoid blocking JVM shutdown
            closeThread.start();
            try {
                closeThread.join(timeoutMillis);
            } catch (final InterruptedException ignore) {
                // ignore
            }
        }
    }

    public void send(final byte[] msg) throws ExecutionException, InterruptedException, TimeoutException {
        if (producer != null) {
            producer.send(new ProducerRecord<byte[], byte[]>(topic, msg)).get(timeoutMillis, TimeUnit.MILLISECONDS);
        }
    }

    public void startup() {
        producer = producerFactory.newKafkaProducer(config);
    }

}
