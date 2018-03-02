/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.logging.log4j.pulsar.appender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.FilteredObjectInputStream;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.ProducerConfiguration;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class PulsarAppenderTest {

    private static final String LOG_MESSAGE = "Hello, world!";

    private static Log4jLogEvent createLogEvent() {
        return Log4jLogEvent.newBuilder()
            .setLoggerName(PulsarAppenderTest.class.getName())
            .setLoggerFqcn(PulsarAppenderTest.class.getName())
            .setLevel(Level.INFO)
            .setMessage(new SimpleMessage(LOG_MESSAGE))
            .build();
    }

    private static PulsarClient client;
    private static Producer producer;
    private static final List<Message> history = new LinkedList<>();

    @Rule
    public LoggerContextRule ctx = new LoggerContextRule("PulsarAppenderTest.xml");

    @BeforeClass
    public static void setUpClass() throws Exception {
        client = mock(PulsarClient.class);
        producer = mock(Producer.class);

        when(client.createProducer(anyString(), any(ProducerConfiguration.class)))
            .thenReturn(producer);

        when(producer.send(any(Message.class)))
            .thenAnswer(invocationOnMock -> {
                Message msg = invocationOnMock.getArgument(0);
                synchronized (history) {
                    history.add(msg);
                }
                return null;
            });

        when(producer.sendAsync(any(Message.class)))
            .thenAnswer(invocationOnMock -> {
                Message msg = invocationOnMock.getArgument(0);
                synchronized (history) {
                    history.add(msg);
                }
                CompletableFuture<MessageId> future = new CompletableFuture<>();
                future.complete(mock(MessageId.class));
                return future;
            });

        PulsarManager.PULSAR_CLIENT_FACTORY = new PulsarFactory() {
            @Override
            public PulsarClient createClient(String serviceUrl) throws Exception {
                return client;
            }
        };
        PulsarManager.MESSAGE_FACTORY = new MessageFactory() {
            @Override
            public Message create(String key, byte[] data) {
                Message msg = mock(Message.class);
                when(msg.getKey()).thenReturn(key);
                when(msg.getData()).thenReturn(data);
                return msg;
            }
        };
    }

    @Before
    public void setUp() throws Exception {
        history.clear();
    }

    @Test
    public void testAppendWithLayout() throws Exception {
        final Appender appender = ctx.getConfiguration().getAppender("PulsarAppenderWithLayout");
        appender.append(createLogEvent());
        final Message item;
        synchronized (history) {
            assertEquals(1, history.size());
            item = history.get(0);
        }
        assertNotNull(item);
        assertNull(item.getKey());
        assertEquals("[" + LOG_MESSAGE + "]", new String(item.getData(), StandardCharsets.UTF_8));
    }

    @Test
    public void testAppendWithSerializedLayout() throws Exception {
        final Appender appender = ctx.getConfiguration().getAppender("PulsarAppenderWithSerializedLayout");
        final LogEvent logEvent = createLogEvent();
        appender.append(logEvent);
        final Message item;
        synchronized (history) {
            assertEquals(1, history.size());
            item = history.get(0);
        }
        assertNotNull(item);
        assertNull(item.getKey());
        assertEquals(LOG_MESSAGE, deserializeLogEvent(item.getData()).getMessage().getFormattedMessage());
    }

    @Test
    public void testAsyncAppend() throws Exception {
        final Appender appender = ctx.getConfiguration().getAppender("AsyncPulsarAppender");
        appender.append(createLogEvent());
        final Message item;
        synchronized (history) {
            assertEquals(1, history.size());
            item = history.get(0);
        }
        assertNotNull(item);
        assertNull(item.getKey());
        assertEquals(LOG_MESSAGE, new String(item.getData(), StandardCharsets.UTF_8));
    }

    @Test
    public void testAppendWithKey() throws Exception {
        final Appender appender = ctx.getConfiguration().getAppender("PulsarAppenderWithKey");
        final LogEvent logEvent = createLogEvent();
        appender.append(logEvent);
        Message item;
        synchronized (history) {
            assertEquals(1, history.size());
            item = history.get(0);
        }
        assertNotNull(item);
        String msgKey = item.getKey();
        assertEquals(msgKey, "key");
        assertEquals(LOG_MESSAGE, new String(item.getData(), StandardCharsets.UTF_8));
    }

    @Test
    public void testAppendWithKeyLookup() throws Exception {
        final Appender appender = ctx.getConfiguration().getAppender("PulsarAppenderWithKeyLookup");
        final LogEvent logEvent = createLogEvent();
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        appender.append(logEvent);
        Message item;
        synchronized (history) {
            assertEquals(1, history.size());
            item = history.get(0);
        }
        assertNotNull(item);
        String keyValue = format.format(date);
        assertEquals(item.getKey(), keyValue);
        assertEquals(LOG_MESSAGE, new String(item.getData(), StandardCharsets.UTF_8));
    }

    private LogEvent deserializeLogEvent(final byte[] data) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(data);
        try (ObjectInput ois = new FilteredObjectInputStream(bis)) {
            return (LogEvent) ois.readObject();
        }
    }

}