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
package org.apache.logging.log4j.flume.appender;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.apache.flume.Channel;
import org.apache.flume.ChannelException;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.channel.ReplicatingChannelSelector;
import org.apache.flume.conf.Configurables;
import org.apache.flume.lifecycle.LifecycleController;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.flume.source.AvroSource;
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class FlumeAppenderTest {

    private AvroSource eventSource;
    private Channel channel;
    private Logger avroLogger;
    private String testPort;

    @BeforeClass
    public static void setupClass() {
        StatusLogger.getLogger().setLevel(Level.OFF);
    }

    @Before
    public void setUp() throws Exception {
        eventSource = new AvroSource();
        channel = new MemoryChannel();

        Configurables.configure(channel, new Context());

        avroLogger = (Logger) LogManager.getLogger("avrologger");
        /*
         * Clear out all other appenders associated with this logger to ensure
         * we're only hitting the Avro appender.
         */
        removeAppenders(avroLogger);
        final Context context = new Context();
        testPort = String.valueOf(AvailablePortFinder.getNextAvailable());
        context.put("port", testPort);
        context.put("bind", "0.0.0.0");
        Configurables.configure(eventSource, context);

        final List<Channel> channels = new ArrayList<>();
        channels.add(channel);

        final ChannelSelector cs = new ReplicatingChannelSelector();
        cs.setChannels(channels);

        eventSource.setChannelProcessor(new ChannelProcessor(cs));

        eventSource.start();

        Assert.assertTrue("Reached start or error", LifecycleController
                .waitForOneOf(eventSource, LifecycleState.START_OR_ERROR));
        Assert.assertEquals("Server is started", LifecycleState.START,
                eventSource.getLifecycleState());
    }

    @After
    public void teardown() throws Exception {
        removeAppenders(avroLogger);
        eventSource.stop();
        Assert.assertTrue("Reached stop or error", LifecycleController
                .waitForOneOf(eventSource, LifecycleState.STOP_OR_ERROR));
        Assert.assertEquals("Server is stopped", LifecycleState.STOP,
                eventSource.getLifecycleState());
    }

    @Test
    public void testLog4jAvroAppender() throws IOException {
        final Agent[] agents = new Agent[] { Agent.createAgent("localhost",
                testPort) };
        final FlumeAppender avroAppender = FlumeAppender.createAppender(agents,
                null, null, "false", "Avro", null, "1000", "1000", "1", "1000",
                "avro", "false", null, null, null, null, null, "true", "1",
                null, null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        Assert.assertNotNull(avroLogger);

        avroLogger.info("Test message");

        final Transaction transaction = channel.getTransaction();
        transaction.begin();

        final Event event = channel.take();
        Assert.assertNotNull(event);
        Assert.assertTrue("Channel contained event, but not expected message",
                getBody(event).endsWith("Test message"));
        transaction.commit();
        transaction.close();

        eventSource.stop();
    }

    @Test
    public void testLog4jAvroAppenderWithHostsParam() throws IOException {
        final String hosts = String.format("localhost:%s", testPort);
        final FlumeAppender avroAppender = FlumeAppender.createAppender(null,
                null, hosts, "false", "Avro", null, "1000", "1000", "1", "1000",
                "avro", "false", null, null, null, null, null, "true", "1",
                null, null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        Assert.assertNotNull(avroLogger);

        avroLogger.info("Test message");

        final Transaction transaction = channel.getTransaction();
        transaction.begin();

        final Event event = channel.take();
        Assert.assertNotNull(event);
        Assert.assertTrue("Channel contained event, but not expected message",
                getBody(event).endsWith("Test message"));
        transaction.commit();
        transaction.close();

        eventSource.stop();
    }

    @Test
    public void testStructured() throws IOException {
        final Agent[] agents = new Agent[] { Agent.createAgent("localhost",
                testPort) };
        final FlumeAppender avroAppender = FlumeAppender.createAppender(agents,
                null, null, "false", "Avro", null, "1000", "1000", "1", "1000",
                "avro", "false", null, null, null, "ReqCtx_", null, "true",
                "1", null, null, null, null);
        avroAppender.start();
        final Logger eventLogger = (Logger) LogManager.getLogger("EventLogger");
        Assert.assertNotNull(eventLogger);
        eventLogger.addAppender(avroAppender);
        eventLogger.setLevel(Level.ALL);

        final StructuredDataMessage msg = new StructuredDataMessage("Transfer",
                "Success", "Audit");
        msg.put("memo", "This is a memo");
        msg.put("acct", "12345");
        msg.put("amount", "100.00");
        ThreadContext.put("id", UUID.randomUUID().toString());
        ThreadContext.put("memo", null);
        ThreadContext.put("test", "123");

        EventLogger.logEvent(msg);

        final Transaction transaction = channel.getTransaction();
        transaction.begin();

        final Event event = channel.take();
        Assert.assertNotNull(event);
        Assert.assertTrue("Channel contained event, but not expected message", getBody(event).endsWith("Success"));
        transaction.commit();
        transaction.close();

        eventSource.stop();
        eventLogger.removeAppender(avroAppender);
        avroAppender.stop();
    }

    @Test
    public void testMultiple() throws IOException {
        final Agent[] agents = new Agent[] { Agent.createAgent("localhost",
                testPort) };
        final FlumeAppender avroAppender = FlumeAppender.createAppender(agents,
                null, null, "false", "Avro", null, "1000", "1000", "1", "1000",
                "avro", "false", null, null, null, null, null, "true", "1",
                null, null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        Assert.assertNotNull(avroLogger);

        for (int i = 0; i < 10; ++i) {
            avroLogger.info("Test message " + i);
        }

        for (int i = 0; i < 10; ++i) {
            final Transaction transaction = channel.getTransaction();
            transaction.begin();

            final Event event = channel.take();
            Assert.assertNotNull(event);
            Assert.assertTrue(
                    "Channel contained event, but not expected message",
                    getBody(event).endsWith("Test message " + i));
            transaction.commit();
            transaction.close();
        }

        eventSource.stop();
    }

    //@Ignore //(Remko: this test hangs my build...)
    @Test
    public void testIncompleteBatch() throws IOException {
        final Agent[] agents = new Agent[] { Agent.createAgent("localhost",
                testPort) };
        final FlumeAppender avroAppender = FlumeAppender.createAppender(agents,
                null, null, "false", "Avro", null, "1000", "1000", "1", "500",
                "avro", "false", null, null, null, null, null, "true", "10",
                null, null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        Assert.assertNotNull(avroLogger);

        avroLogger.info("Test message 0");

        final Transaction transaction = channel.getTransaction();
        transaction.begin();

        Event event = channel.take();
        Assert.assertNull("Received event", event);

        try {
            Thread.sleep(500);
        } catch (final InterruptedException ie) {
        }

        avroLogger.info("Test message 1");
        for (int i = 0; i < 2; ++i) {
            event = channel.take();
            Assert.assertNotNull("No event for item " + i, event);
            Assert.assertTrue("Channel contained event, but not expected message",
                    getBody(event).endsWith("Test message " + i));
        }
        transaction.commit();
        transaction.close();

        eventSource.stop();
    }

    @Test
    public void testIncompleteBatch2() throws IOException {
        final Agent[] agents = new Agent[] { Agent.createAgent("localhost",
                testPort) };
        final FlumeAppender avroAppender = FlumeAppender.createAppender(agents,
                null, null, "false", "Avro", null, "1000", "1000", "1", "500",
                "avro", "false", null, null, null, null, null, "true", "10",
                null, null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        Assert.assertNotNull(avroLogger);

        avroLogger.info("Test message 0");

        final Transaction transaction = channel.getTransaction();
        transaction.begin();

        avroLogger.info("Test message 1");
        avroLogger.info("Test message 2");
        avroAppender.stop();
        for (int i = 0; i < 3; ++i) {
            final Event event = channel.take();
            Assert.assertNotNull("No event for item " + i, event);
            Assert.assertTrue("Channel contained event, but not expected message. Received : " + getBody(event),
                    getBody(event).endsWith("Test message " + i));
        }
        transaction.commit();
        transaction.close();

        eventSource.stop();
    }

    @Test
    public void testBatch() throws IOException {
        final Agent[] agents = new Agent[] { Agent.createAgent("localhost",
                testPort) };
        final FlumeAppender avroAppender = FlumeAppender.createAppender(agents,
                null, null, "false", "Avro", null, "1000", "1000", "1", "1000",
                "avro", "false", null, null, null, null, null, "true", "10",
                null, null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        Assert.assertNotNull(avroLogger);

        for (int i = 0; i < 10; ++i) {
            avroLogger.info("Test message " + i);
        }

        final Transaction transaction = channel.getTransaction();
        transaction.begin();

        for (int i = 0; i < 10; ++i) {
            final Event event = channel.take();
            Assert.assertNotNull("No event for item " + i, event);
            Assert.assertTrue(
                    "Channel contained event, but not expected message",
                    getBody(event).endsWith("Test message " + i));
        }
        transaction.commit();
        transaction.close();

        eventSource.stop();
    }

    @Test
    public void testConnectionRefused() {
        final Agent[] agents = new Agent[] { Agent.createAgent("localhost",
                testPort) };
        final FlumeAppender avroAppender = FlumeAppender.createAppender(agents,
                null, null, "false", "Avro", null, "1000", "1000", "1", "1000",
                "avro", "false", null, null, null, null, null, "true", "1",
                null, null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);
        eventSource.stop();

        boolean caughtException = false;

        try {
            avroLogger.info("message 1");
        } catch (final Throwable t) {
            // logger.debug("Logging to a non-existent server failed (as expected)",
            // t);

            caughtException = true;
        }

        Assert.assertTrue(caughtException);
    }

    @Test
    public void testNotConnected() throws Exception {
        eventSource.stop();
        final String altPort = Integer.toString(Integer.parseInt(testPort) + 1);
        final Agent[] agents = new Agent[] {
                Agent.createAgent("localhost", testPort),
                Agent.createAgent("localhost", altPort) };
        final FlumeAppender avroAppender = FlumeAppender.createAppender(agents,
                null, null, "false", "Avro", null, "1000", "1000", "1", "1000",
                "avro", "false", null, null, null, null, null, "true", "1",
                null, null, null, null);
        avroAppender.start();
        Assert.assertTrue("Appender Not started", avroAppender.isStarted());
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        try {
            avroLogger.info("Test message");
            Assert.fail("Exception should have been thrown");
        } catch (final Exception ex) {

        }

        try {
            final Context context = new Context();
            context.put("port", altPort);
            context.put("bind", "0.0.0.0");

            Configurables.configure(eventSource, context);

            eventSource.start();
        } catch (final ChannelException e) {
            Assert.fail("Caught exception while resetting port to " + altPort
                    + " : " + e.getMessage());
        }

        avroLogger.info("Test message 2");

        final Transaction transaction = channel.getTransaction();
        transaction.begin();

        final Event event = channel.take();
        Assert.assertNotNull(event);
        Assert.assertTrue("Channel contained event, but not expected message",
                getBody(event).endsWith("Test message 2"));
        transaction.commit();
        transaction.close();
    }

    @Test
    public void testReconnect() throws Exception {
        final String altPort = Integer.toString(Integer.parseInt(testPort) + 1);
        final Agent[] agents = new Agent[] {
                Agent.createAgent("localhost", testPort),
                Agent.createAgent("localhost", altPort) };
        final FlumeAppender avroAppender = FlumeAppender.createAppender(agents,
                null, null, "false", "Avro", null, "1000", "1000", "1", "1000",
                "avro", "false", null, null, null, null, null, "true", "1",
                null, null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        avroLogger.info("Test message");

        Transaction transaction = channel.getTransaction();
        transaction.begin();

        Event event = channel.take();
        Assert.assertNotNull(event);
        Assert.assertTrue("Channel contained event, but not expected message. Received : " + getBody(event),
                getBody(event).endsWith("Test message"));
        transaction.commit();
        transaction.close();

        eventSource.stop();
        try {
            final Context context = new Context();
            context.put("port", altPort);
            context.put("bind", "0.0.0.0");

            Configurables.configure(eventSource, context);

            eventSource.start();
        } catch (final ChannelException e) {
            Assert.fail("Caught exception while resetting port to " + altPort
                    + " : " + e.getMessage());
        }

        avroLogger.info("Test message 2");

        transaction = channel.getTransaction();
        transaction.begin();

        event = channel.take();
        Assert.assertNotNull(event);
        Assert.assertTrue("Channel contained event, but not expected message",
                getBody(event).endsWith("Test message 2"));
        transaction.commit();
        transaction.close();
    }

    private void removeAppenders(final Logger logger) {
        final Map<String, Appender> map = logger.getAppenders();
        for (final Map.Entry<String, Appender> entry : map.entrySet()) {
            final Appender app = entry.getValue();
            avroLogger.removeAppender(app);
            app.stop();
        }
    }

    private String getBody(final Event event) throws IOException {
        if (event == null) {
            return "";
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final InputStream is = new GZIPInputStream(new ByteArrayInputStream(
                event.getBody()));
        int n = 0;
        while (-1 != (n = is.read())) {
            baos.write(n);
        }
        return new String(baos.toByteArray());

    }
}
