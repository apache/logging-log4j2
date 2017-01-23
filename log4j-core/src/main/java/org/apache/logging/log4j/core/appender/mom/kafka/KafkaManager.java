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

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.Log4jThread;

public class KafkaManager extends AbstractManager {

    public static final String DEFAULT_TIMEOUT_MILLIS = "30000";

    /**
     * package-private access for testing.
     */
    static KafkaProducerFactory producerFactory = new DefaultKafkaProducerFactory();

    private final Properties config = new Properties();
    private Producer<byte[], byte[]> producer;
    private final int timeoutMillis;

    private final String topic;
    private final boolean syncSend;

    public KafkaManager(final LoggerContext loggerContext, final String name, final String topic, final boolean syncSend, final Property[] properties) {
        super(loggerContext, name);
        this.topic = Objects.requireNonNull(topic, "topic");
        this.syncSend = syncSend;
        config.setProperty("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        config.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        config.setProperty("batch.size", "0");
        for (final Property property : properties) {
            config.setProperty(property.getName(), property.getValue());
        }
        this.timeoutMillis = Integer.parseInt(config.getProperty("timeout.ms", DEFAULT_TIMEOUT_MILLIS));
    }

    @Override
    public boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        if (timeout > 0) {
            closeProducer(timeout, timeUnit);
        } else {
            closeProducer(timeoutMillis, TimeUnit.MILLISECONDS);
        }
        return true;
    }

    private void closeProducer(final long timeout, final TimeUnit timeUnit) {
        if (producer != null) {
            // This thread is a workaround for this Kafka issue: https://issues.apache.org/jira/browse/KAFKA-1660
           final Thread closeThread = new Log4jThread(new Runnable() {
                @Override
                public void run() {
                    if (producer != null) {
                        producer.close();
                    }
                }
            }, "KafkaManager-CloseThread");
            closeThread.setDaemon(true); // avoid blocking JVM shutdown
            closeThread.start();
            try {
                closeThread.join(timeUnit.toMillis(timeout));
            } catch (final InterruptedException ignore) {
                Thread.currentThread().interrupt();
                // ignore
            }
        }
    }

    public void send(final byte[] msg) throws ExecutionException, InterruptedException, TimeoutException {
        if (producer != null) {
            ProducerRecord<byte[], byte[]> newRecord = new ProducerRecord<>(topic, msg);
            if (syncSend) {
                Future<RecordMetadata> response = producer.send(newRecord);
                response.get(timeoutMillis, TimeUnit.MILLISECONDS);
            } else {
                producer.send(newRecord, new Callback() {
                    public void onCompletion(RecordMetadata metadata, Exception e) {
                        if (e != null) {
                            LOGGER.error("Unable to write to Kafka [" + getName() + "].", e);
                        }
                    }
                });
            }
        }
    }

    public void startup() {
        producer = producerFactory.newKafkaProducer(config);
    }

    public String getTopic() {
        return topic;
    }

}
