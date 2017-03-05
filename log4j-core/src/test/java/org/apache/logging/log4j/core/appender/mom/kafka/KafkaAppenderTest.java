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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.categories.Appenders;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

@Category(Appenders.Kafka.class)
public class KafkaAppenderTest {

    private static final MockProducer<byte[], byte[]> kafka = new MockProducer<byte[], byte[]>(true, null, null) {
        @Override
        public void close() {
            try {
                Thread.sleep(3000);
            } catch (final InterruptedException ignore) {
            }
        }

        @Override
        public void close(final long timeout, final TimeUnit timeUnit) {
            try {
                Thread.sleep(timeUnit.toMillis(timeout));
            } catch (final InterruptedException ignore) {
            }
        }
    };

    private static final String LOG_MESSAGE = "Hello, world!";
    private static final String TOPIC_NAME = "kafka-topic";

    private static Log4jLogEvent createLogEvent() {
        return Log4jLogEvent.newBuilder()
            .setLoggerName(KafkaAppenderTest.class.getName())
            .setLoggerFqcn(KafkaAppenderTest.class.getName())
            .setLevel(Level.INFO)
            .setMessage(new SimpleMessage(LOG_MESSAGE))
            .build();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        KafkaManager.producerFactory = new KafkaProducerFactory() {
            @Override
            public Producer<byte[], byte[]> newKafkaProducer(final Properties config) {
                return kafka;
            }
        };
    }

    @Rule
    public LoggerContextRule ctx = new LoggerContextRule("KafkaAppenderTest.xml");

    @Before
    public void setUp() throws Exception {
        kafka.clear();
    }

    @Test
    public void testAppend() throws Exception {
        final Appender appender = ctx.getRequiredAppender("KafkaAppender");
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
    public void testAppendWithLayout() throws Exception {
        final Appender appender = ctx.getRequiredAppender("KafkaAppenderWithLayout");
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
    public void testAppendWithSerializedLayout() throws Exception {
        final Appender appender = ctx.getRequiredAppender("KafkaAppenderWithSerializedLayout");
        final LogEvent logEvent = createLogEvent();
        appender.append(logEvent);
        final List<ProducerRecord<byte[], byte[]>> history = kafka.history();
        assertEquals(1, history.size());
        final ProducerRecord<byte[], byte[]> item = history.get(0);
        assertNotNull(item);
        assertEquals(TOPIC_NAME, item.topic());
        assertNull(item.key());
        assertEquals(LOG_MESSAGE, deserializeLogEvent(item.value()).getMessage().getFormattedMessage());
    }

    @Test
    public void testAsyncAppend() throws Exception {
        final Appender appender = ctx.getRequiredAppender("AsyncKafkaAppender");
        appender.append(createLogEvent());
        final List<ProducerRecord<byte[], byte[]>> history = kafka.history();
        assertEquals(1, history.size());
        final ProducerRecord<byte[], byte[]> item = history.get(0);
        assertNotNull(item);
        assertEquals(TOPIC_NAME, item.topic());
        assertNull(item.key());
        assertEquals(LOG_MESSAGE, new String(item.value(), StandardCharsets.UTF_8));
    }

    private LogEvent deserializeLogEvent(final byte[] data) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(data);
        try (ObjectInput ois = new ObjectInputStream(bis)) {
            return (LogEvent) ois.readObject();
        }
    }

    @Test(timeout = 2000)
    public void testClose() throws Exception {
        final Appender appender = ctx.getRequiredAppender("KafkaAppender");
        appender.stop();
    }
}