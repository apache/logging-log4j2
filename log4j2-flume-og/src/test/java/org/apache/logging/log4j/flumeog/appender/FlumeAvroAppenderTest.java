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
package org.apache.logging.log4j.flumeog.appender;

import com.cloudera.flume.core.Event;
import com.cloudera.flume.handlers.avro.AvroEventSource;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;

/**
 *
 */
public class FlumeAvroAppenderTest {

    private LoggerContext ctx = (LoggerContext) LogManager.getContext();

    private static final String LOGBACK_CONF = "logback.configurationFile";
    private static final String LOGBACK_CONFIG = "logback-flume.xml";

    private static final int testServerPort = 12345;
    private static final int testEventCount = 100;

    private AvroEventSource eventSource;
    private Logger avroLogger;

    @BeforeClass
    public static void setupClass() {
        System.setProperty(LOGBACK_CONF, LOGBACK_CONFIG);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(LOGBACK_CONF);
    }

    @Before
    public void setUp() throws IOException {
        eventSource = new AvroEventSource(testServerPort);
        avroLogger = (Logger) LogManager.getLogger("avrologger");
        /*
        * Clear out all other appenders associated with this logger to ensure we're
        * only hitting the Avro appender.
        */
        removeAppenders(avroLogger);
        eventSource.open();
    }

    @After
    public void teardown() throws IOException {
        removeAppenders(avroLogger);
        eventSource.close();
    }

    @Test
    public void testLog4jAvroAppender() throws InterruptedException, IOException {
        Agent[] agents = new Agent[] {Agent.createAgent("localhost", Integer.toString(testServerPort))};
        FlumeAvroAppender avroAppender = FlumeAvroAppender.createAppender(agents, "100", "3", "avro", "false", null,
            null, null, null, null, "true", null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        Assert.assertNotNull(avroLogger);

        int loggedCount = 0;
        int receivedCount = 0;

        for (int i = 0; i < testEventCount; i++) {
            avroLogger.info("test i:" + i);
            loggedCount++;
        }

        /*
        * We perform this in another thread so we can put a time SLA on it by using
        * Future#get(). Internally, the AvroEventSource uses a BlockingQueue.
        */
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Event> callable = new Callable<Event>() {

            public Event call() throws Exception {
                return eventSource.next();
            }
        };

        for (int i = 0; i < loggedCount; i++) {
            try {
                Future<Event> future = executor.submit(callable);

                /*
                * We must receive events in less than 1 second. This should be more
                * than enough as all events should be held in AvroEventSource's
                * BlockingQueue.
                */
                Event event = future.get(1, TimeUnit.SECONDS);

                Assert.assertNotNull(event);
                Assert.assertNotNull(event.getBody());
                String body = getBody(event);
                Assert.assertTrue(body.endsWith("test i:" + i));

                receivedCount++;
            } catch (ExecutionException e) {
                Assert.fail("Flume failed to handle an event: " + e.getMessage());
                break;
            } catch (TimeoutException e) {
                Assert
                    .fail("Flume failed to handle an event within the given time SLA: "
                        + e.getMessage());
                break;
            } catch (InterruptedException e) {
                Assert
                    .fail("Flume source executor thread was interrupted. We count this as a failure.");
                Thread.currentThread().interrupt();
                break;
            }
        }

        executor.shutdown();

        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException(
                "Executor is refusing to shutdown cleanly");
        }

        Assert.assertEquals(loggedCount, receivedCount);
    }

    @Test
    public void testConnectionRefused() {
        Agent[] agents = new Agent[] {Agent.createAgent("localhost", Integer.toString(44000))};
        FlumeAvroAppender avroAppender = FlumeAvroAppender.createAppender(agents, "100", "3", "avro", "false", null,
            null, null, null, null, "true", null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);

        boolean caughtException = false;

        try {
            avroLogger.info("message 1");
        } catch (Throwable t) {
            //logger.debug("Logging to a non-existant server failed (as expected)", t);

            caughtException = true;
        }

        Assert.assertTrue(caughtException);
    }

    @Test
    public void testReconnect() throws IOException {
        Agent[] agents = new Agent[] {Agent.createAgent("localhost", Integer.toString(testServerPort))};
        FlumeAvroAppender avroAppender = FlumeAvroAppender.createAppender(agents, "500", "10", "avro", "false", null,
            null, null, null, null, "true", null, null, null);
        avroAppender.start();
        avroLogger.addAppender(avroAppender);
        avroLogger.setLevel(Level.ALL);
        avroLogger.info("message 1");

        Event event = eventSource.next();

        Assert.assertNotNull(event);
        String body = getBody(event);
        Assert.assertTrue(body.endsWith("message 1"));

        eventSource.close();

        Callable<Void> logCallable = new Callable<Void>() {

            public Void call() throws Exception {
                avroLogger.info("message 2");
                return null;
            }
        };

        ExecutorService logExecutor = Executors.newSingleThreadExecutor();

        boolean caughtException = false;

        try {
            logExecutor.submit(logCallable);

            Thread.sleep(1500);

            eventSource.open();

            logExecutor.shutdown();

            if (!logExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                    "Log executor is refusing to shutdown cleanly");
            }
        } catch (Throwable t) {
            System.err.println("Failed to reestablish a connection and log to an avroSource");

            caughtException = true;
        }

        Assert.assertFalse(caughtException);

        event = eventSource.next();

        Assert.assertNotNull(event);
        body = getBody(event);
        Assert.assertTrue(body.endsWith("message 2"));

        caughtException = false;

        try {
            avroLogger.info("message 3");
        } catch (Throwable t) {
            System.err.println("Logging to a closed server failed (not expected)");

            caughtException = true;
        }

        Assert.assertFalse(caughtException);

        event = eventSource.next();

        Assert.assertNotNull(event);
        body = getBody(event);
        Assert.assertTrue(body.endsWith("message 3"));
    }


    private void removeAppenders(Logger logger) {
        Map<String,Appender> map = logger.getAppenders();
        for (Map.Entry<String, Appender> entry : map.entrySet()) {
            Appender app = entry.getValue();
            avroLogger.removeAppender(app);
            app.stop();
        }
    }

    private Appender getAppender(Logger logger, String name) {
        Map<String,Appender> map = logger.getAppenders();
        return map.get(name);
    }

    private String getBody(Event event) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = new GZIPInputStream(new ByteArrayInputStream(event.getBody()));
            int n = 0;
            while (-1 != (n = is.read())) {
                baos.write(n);
            }
            return new String(baos.toByteArray());

    }
}
