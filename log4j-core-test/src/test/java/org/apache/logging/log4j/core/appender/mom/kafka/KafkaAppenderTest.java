/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender.mom.kafka;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.junit.SerialUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Appenders.Kafka")
public class KafkaAppenderTest {

    private static final Serializer<byte[]> SERIALIZER = new ByteArraySerializer();

    private static final MockProducer<byte[], byte[]> kafka =
            new MockProducer<byte[], byte[]>(true, SERIALIZER, SERIALIZER) {

                @Override
                public synchronized Future<RecordMetadata> send(final ProducerRecord<byte[], byte[]> record) {

                    final Future<RecordMetadata> retVal = super.send(record);

                    final boolean isRetryTest = "true".equals(ThreadContext.get("KafkaAppenderWithRetryCount"));
                    if (isRetryTest) {
                        try {
                            throw new TimeoutException();
                        } catch (TimeoutException e) {
                            // TODO Auto-generated catch block
                            throw new RuntimeException(e);
                        }
                    }

                    return retVal;
                }

                // @Override in version 1.1.1
                public void close(final long timeout, final TimeUnit timeUnit) {
                    // Intentionally do not close in order to reuse
                }

                // @Override in version 3.3.1
                public void close(final Duration timeout) {
                    // Intentionally do no close in order to reuse
                }
            };

    private static final String LOG_MESSAGE = "Hello, world!";
    private static final String TOPIC_NAME = "kafka-topic";
    private static final int RETRY_COUNT = 3;

    private static Log4jLogEvent createLogEvent() {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(KafkaAppenderTest.class.getName())
                .setLoggerFqcn(KafkaAppenderTest.class.getName())
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage(LOG_MESSAGE))
                .build();
    }

    @BeforeAll
    public static void setUpAll() {
        KafkaManager.producerFactory = config -> kafka;
    }

    @BeforeEach
    public void setUp() {
        kafka.clear();
    }

    @Test
    @LoggerContextSource("KafkaAppenderTest.xml")
    public void testAppendWithLayout(@Named("KafkaAppenderWithLayout") final Appender appender) {
        assertNotNull(appender, "Appender named KafkaAppenderWithLayout was null.");
        appender.append(createLogEvent());
        final List<ProducerRecord<byte[], byte[]>> history = kafka.history();
        assertEquals(1, history.size());
        final ProducerRecord<byte[], byte[]> item = history.get(0);
        assertNotNull(item);
        assertEquals(TOPIC_NAME, item.topic());
        assertNull(item.key());
        assertEquals("[" + LOG_MESSAGE + "]", new String(item.value(), StandardCharsets.UTF_8));
    }

    @Test
    @LoggerContextSource("KafkaAppenderTest.xml")
    public void testAppendWithSerializedLayout(@Named("KafkaAppenderWithSerializedLayout") final Appender appender) {
        assertNotNull(appender, "Appender named KafkaAppenderWithSerializedLayout was null.");
        final LogEvent logEvent = createLogEvent();
        appender.append(logEvent);
        final List<ProducerRecord<byte[], byte[]>> history = kafka.history();
        assertEquals(1, history.size());
        final ProducerRecord<byte[], byte[]> item = history.get(0);
        assertNotNull(item);
        assertEquals(TOPIC_NAME, item.topic());
        assertNull(item.key());
        final byte[] data = item.value();
        assertEquals(
                LOG_MESSAGE, SerialUtil.<LogEvent>deserialize(data).getMessage().getFormattedMessage());
    }

    @Test
    @LoggerContextSource("KafkaAppenderTest.xml")
    public void testAsyncAppend(@Named("AsyncKafkaAppender") final Appender appender) {
        assertNotNull(appender, "Appender named AsyncKafkaAppender was null.");
        appender.append(createLogEvent());
        final List<ProducerRecord<byte[], byte[]>> history = kafka.history();
        assertEquals(1, history.size());
        final ProducerRecord<byte[], byte[]> item = history.get(0);
        assertNotNull(item);
        assertEquals(TOPIC_NAME, item.topic());
        assertNull(item.key());
        assertEquals(LOG_MESSAGE, new String(item.value(), StandardCharsets.UTF_8));
    }

    @Test
    @LoggerContextSource("KafkaAppenderTest.xml")
    public void testAppendWithKey(@Named("KafkaAppenderWithKey") final Appender appender) {
        assertNotNull(appender, "Appender named KafkaAppenderWithKey was null.");
        final LogEvent logEvent = createLogEvent();
        appender.append(logEvent);
        final List<ProducerRecord<byte[], byte[]>> history = kafka.history();
        assertEquals(1, history.size());
        final ProducerRecord<byte[], byte[]> item = history.get(0);
        assertNotNull(item);
        assertEquals(TOPIC_NAME, item.topic());
        final byte[] keyValue = "key".getBytes(StandardCharsets.UTF_8);
        assertEquals(Long.valueOf(logEvent.getTimeMillis()), item.timestamp());
        assertArrayEquals(item.key(), keyValue);
        assertEquals(LOG_MESSAGE, new String(item.value(), StandardCharsets.UTF_8));
    }

    @Test
    @LoggerContextSource("KafkaAppenderTest.xml")
    public void testAppendWithKeyLookup(@Named("KafkaAppenderWithKeyLookup") final Appender appender) {
        assertNotNull(appender, "Appender named KafkaAppenderWithKeyLookup was null.");
        final LogEvent logEvent = createLogEvent();
        final Date date = new Date();
        final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        appender.append(logEvent);
        final List<ProducerRecord<byte[], byte[]>> history = kafka.history();
        assertEquals(1, history.size());
        final ProducerRecord<byte[], byte[]> item = history.get(0);
        assertNotNull(item);
        assertEquals(TOPIC_NAME, item.topic());
        final byte[] keyValue = format.format(date).getBytes(StandardCharsets.UTF_8);
        assertEquals(Long.valueOf(logEvent.getTimeMillis()), item.timestamp());
        assertArrayEquals(item.key(), keyValue);
        assertEquals(LOG_MESSAGE, new String(item.value(), StandardCharsets.UTF_8));
    }

    @Test
    @LoggerContextSource("KafkaAppenderTest.xml")
    public void testAppendWithRetryCount(@Named("KafkaAppenderWithRetryCount") final Appender appender) {
        assertNotNull(appender, "Appender named KafkaAppenderWithRetryCount was null.");
        try {
            ThreadContext.put("KafkaAppenderWithRetryCount", "true");
            final LogEvent logEvent = createLogEvent();
            appender.append(logEvent);

            final List<ProducerRecord<byte[], byte[]>> history = kafka.history();
            assertEquals(RETRY_COUNT + 1, history.size());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ThreadContext.clearMap();
        }
    }

    @Test
    @LoggerContextSource("KafkaAppenderTest.xml")
    public void testAppenderNoEventTimestamp(@Named("KafkaAppenderNoEventTimestamp") final Appender appender) {
        assertNotNull(appender, "Appender named KafkaAppenderNoEventTimestamp was null.");
        final LogEvent logEvent = createLogEvent();
        appender.append(logEvent);
        final List<ProducerRecord<byte[], byte[]>> history = kafka.history();
        assertEquals(1, history.size());
        final ProducerRecord<byte[], byte[]> item = history.get(0);
        assertNotNull(item);
        assertEquals(TOPIC_NAME, item.topic());
        final byte[] keyValue = "key".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(item.key(), keyValue);
        assertNotEquals(Long.valueOf(logEvent.getTimeMillis()), item.timestamp());
        assertEquals(LOG_MESSAGE, new String(item.value(), StandardCharsets.UTF_8));
    }

    //    public void shouldRetryWhenTimeoutExceptionOccursOnSend() throws Exception {
    //        final AtomicInteger attempt = new AtomicInteger(0);
    //        final RecordCollectorImpl collector = new RecordCollectorImpl(
    //                new MockProducer(cluster, true, new DefaultPartitioner(), byteArraySerializer,
    // byteArraySerializer) {
    //                    @Override
    //                    public synchronized Future<RecordMetadata> send(final ProducerRecord record, final Callback
    // callback) {
    //                        if (attempt.getAndIncrement() == 0) {
    //                            throw new TimeoutException();
    //                        }
    //                        return super.send(record, callback);
    //                    }
    //                },
    //                "test");
    //
    //        collector.send("topic1", "3", "0", null, stringSerializer, stringSerializer, streamPartitioner);
    //        final Long offset = collector.offsets().get(new TopicPartition("topic1", 0));
    //        assertEquals(Long.valueOf(0L), offset);
    //    }

}
