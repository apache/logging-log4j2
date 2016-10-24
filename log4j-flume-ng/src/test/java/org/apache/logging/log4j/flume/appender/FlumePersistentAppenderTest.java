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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.NettyServer;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.flume.Event;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.avro.AvroFlumeEvent;
import org.apache.flume.source.avro.AvroSourceProtocol;
import org.apache.flume.source.avro.Status;
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Preconditions;

/**
 *
 */
public class FlumePersistentAppenderTest {
    private static final String CONFIG = "persistent.xml";
    private static final String HOSTNAME = "localhost";
    private static LoggerContext ctx;

    private EventCollector primary;
    private EventCollector alternate;

    @BeforeClass
    public static void setupClass() {
        // System.setProperty(DefaultConfiguration.DEFAULT_LEVEL, Level.DEBUG.toString());
        final File file = new File("target/file-channel");
        if (!deleteFiles(file)) {
            System.err.println("Warning - unable to delete target/file-channel. Test errors may occur");
        }
    }

    @AfterClass
    public static void cleanupClass() {
        StatusLogger.getLogger().reset();
    }

    @Before
    public void setUp() throws Exception {

        final File file = new File("target/persistent");
        deleteFiles(file);

        /*
        * Clear out all other appenders associated with this logger to ensure we're
        * only hitting the Avro appender.
        */
        final int primaryPort = AvailablePortFinder.getNextAvailable();
        final int altPort = AvailablePortFinder.getNextAvailable();
        System.setProperty("primaryPort", Integer.toString(primaryPort));
        System.setProperty("alternatePort", Integer.toString(altPort));
        primary = new EventCollector(primaryPort);
        alternate = new EventCollector(altPort);
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = LoggerContext.getContext(false);
        ctx.reconfigure();
    }

    @After
    public void teardown() throws Exception {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        primary.stop();
        alternate.stop();
        final File file = new File("target/file-channel");
        deleteFiles(file);
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final Set<ObjectName> names = server.queryNames(new ObjectName("org.apache.flume.*:*"), null);
        for (final ObjectName name : names) {
            try {
                server.unregisterMBean(name);
            } catch (final Exception ex) {
                System.out.println("Unable to unregister " + name.toString());
            }
        }
    }

    @Test
    public void testLog4Event() throws IOException {

        final StructuredDataMessage msg = new StructuredDataMessage("Test", "Test Log4j", "Test");
        EventLogger.logEvent(msg);

        final Event event = primary.poll();
        Assert.assertNotNull(event);
        final String body = getBody(event);
        Assert.assertTrue("Channel contained event, but not expected message. Received: " + body,
            body.endsWith("Test Log4j"));
    }

    @Test
    public void testMultiple() {

        for (int i = 0; i < 10; ++i) {
            final StructuredDataMessage msg = new StructuredDataMessage("Test", "Test Multiple " + i, "Test");
            msg.put("counter", Integer.toString(i));
            EventLogger.logEvent(msg);
        }
        final boolean[] fields = new boolean[10];
        for (int i = 0; i < 10; ++i) {
            final Event event = primary.poll();
            Assert.assertNotNull("Received " + i + " events. Event " + (i + 1) + " is null", event);
            final String value = event.getHeaders().get("counter");
            Assert.assertNotNull("Missing counter", value);
            final int counter = Integer.parseInt(value);
            if (fields[counter]) {
                Assert.fail("Duplicate event");
            } else {
                fields[counter] = true;
            }
        }
        for (int i = 0; i < 10; ++i) {
            Assert.assertTrue("Channel contained event, but not expected message " + i, fields[i]);
        }
    }


    @Test
    public void testFailover() throws InterruptedException {
        final Logger logger = LogManager.getLogger("testFailover");
        logger.debug("Starting testFailover");
        for (int i = 0; i < 10; ++i) {
            final StructuredDataMessage msg = new StructuredDataMessage("Test", "Test Primary " + i, "Test");
            msg.put("counter", Integer.toString(i));
            EventLogger.logEvent(msg);
        }
        boolean[] fields = new boolean[10];
        for (int i = 0; i < 10; ++i) {
            final Event event = primary.poll();
            Assert.assertNotNull("Received " + i + " events. Event " + (i + 1) + " is null", event);
            final String value = event.getHeaders().get("counter");
            Assert.assertNotNull("Missing counter", value);
            final int counter = Integer.parseInt(value);
            if (fields[counter]) {
                Assert.fail("Duplicate event");
            } else {
                fields[counter] = true;
            }
        }
        for (int i = 0; i < 10; ++i) {
            Assert.assertTrue("Channel contained event, but not expected message " + i, fields[i]);
        }

        // Give the AvroSink time to receive notification and notify the channel.
        Thread.sleep(500);

        primary.stop();

        for (int i = 0; i < 10; ++i) {
            final StructuredDataMessage msg = new StructuredDataMessage("Test", "Test Alternate " + i, "Test");
            msg.put("cntr", Integer.toString(i));
            EventLogger.logEvent(msg);
        }
        fields = new boolean[10];
        for (int i = 0; i < 10; ++i) {
            final Event event = alternate.poll();
            Assert.assertNotNull("Received " + i + " events. Event " + (i + 1) + " is null", event);
            final String value = event.getHeaders().get("cntr");
            Assert.assertNotNull("Missing counter", value);
            final int counter = Integer.parseInt(value);
            if (fields[counter]) {
                Assert.fail("Duplicate event");
            } else {
                fields[counter] = true;
            }
        }
        for (int i = 0; i < 10; ++i) {
            Assert.assertTrue("Channel contained event, but not expected message " + i, fields[i]);
        }
    }

    @Test
    public void testSingle() throws IOException {

        final Logger logger = LogManager.getLogger("EventLogger");
        final Marker marker = MarkerManager.getMarker("EVENT");
        logger.info(marker, "This is a test message");

        final Event event = primary.poll();
        Assert.assertNotNull(event);
        final String body = getBody(event);
        Assert.assertTrue("Channel contained event, but not expected message. Received: " + body,
            body.endsWith("This is a test message"));
    }

    @Test
    public void testMultipleConcurrent() throws InterruptedException {

        final int eventsCount = 10000;

        final Thread writer1 = new WriterThread(0, eventsCount / 4);
        final Thread writer2 = new WriterThread(eventsCount / 4, eventsCount / 2);
        final Thread writer3 = new WriterThread(eventsCount / 2, (3 * eventsCount) / 4);
        final Thread writer4 = new WriterThread((3 * eventsCount) / 4, eventsCount);
        writer1.start();
        writer2.start();
        writer3.start();
        writer4.start();


        final boolean[] fields = new boolean[eventsCount];
        final Thread reader1 = new ReaderThread(0, eventsCount / 4, fields);
        final Thread reader2 = new ReaderThread(eventsCount / 4, eventsCount / 2, fields);
        final Thread reader3 = new ReaderThread(eventsCount / 2, (eventsCount * 3) / 4, fields);
        final Thread reader4 = new ReaderThread((eventsCount * 3) / 4, eventsCount, fields);

        reader1.start();
        reader2.start();
        reader3.start();
        reader4.start();

        writer1.join();
        writer2.join();
        writer3.join();
        writer4.join();
        reader1.join();
        reader2.join();
        reader3.join();
        reader4.join();

        for (int i = 0; i < eventsCount; ++i) {
            Assert.assertTrue(
                "Channel contained event, but not expected message " + i,
                fields[i]);
        }
    }
    
    @Test
    public void testRFC5424Layout() throws IOException {

        final StructuredDataMessage msg = new StructuredDataMessage("Test", "Test Log4j", "Test");
        EventLogger.logEvent(msg);

        final Event event = primary.poll();
        Assert.assertNotNull(event);
        final String body = getBody(event);
        Assert.assertTrue("Structured message does not contain @EID: " + body,
            body.contains("Test@18060"));
    }

    private class WriterThread extends Thread {

        private final int start;
        private final int stop;

        public WriterThread(final int start, final int stop) {
            this.start = start;
            this.stop = stop;
        }

        @Override
        public void run() {
            for (int i = start; i < stop; ++i) {
                final StructuredDataMessage msg = new StructuredDataMessage(
                    "Test", "Test Multiple " + i, "Test");
                msg.put("counter", Integer.toString(i));
                EventLogger.logEvent(msg);
            }
        }
    }

    private class ReaderThread extends Thread {
        private final int start;
        private final int stop;
        private final boolean[] fields;

        private ReaderThread(final int start, final int stop, final boolean[] fields) {
            this.start = start;
            this.stop = stop;
            this.fields = fields;
        }
        @Override
        public void run() {

            for (int i = start; i < stop; ++i) {
                Event event = primary.poll();
                while (event == null) {
                    event = primary.poll();
                }

                Assert.assertNotNull("Received " + i + " events. Event "
                    + (i + 1) + " is null", event);
                final String value = event.getHeaders().get("counter");
                Assert.assertNotNull("Missing counter", value);
                final int counter = Integer.parseInt(value);
                if (fields[counter]) {
                    Assert.fail("Duplicate event");
                } else {
                    fields[counter] = true;
                }

            }
        }
    }

    /*
    @Test
    public void testPerformance() throws Exception {
        long start = System.currentTimeMillis();
        int count = 1000;
        for (int i = 0; i < count; ++i) {
            final StructuredDataMessage msg = new StructuredDataMessage("Test", "Test Primary " + i, "Test");
            msg.put("counter", Integer.toString(i));
            EventLogger.logEvent(msg);
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Time to log " + count + " events " + elapsed + "ms");
    }    */


    private String getBody(final Event event) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final InputStream is = new GZIPInputStream(new ByteArrayInputStream(event.getBody()));
        int n = 0;
        while (-1 != (n = is.read())) {
            baos.write(n);
        }
        return new String(baos.toByteArray());

    }

	private static boolean deleteFiles(final File file) {
		boolean result = true;
		if (file.isDirectory()) {

			final File[] files = file.listFiles();
			if (files != null) {
				for (final File child : files) {
					result &= deleteFiles(child);
				}
			}
		} else if (!file.exists()) {
			return true;
		}

		return result && file.delete();
	}

    private static class EventCollector implements AvroSourceProtocol {
        private final LinkedBlockingQueue<AvroFlumeEvent> eventQueue = new LinkedBlockingQueue<>();

        private final NettyServer nettyServer;


        public EventCollector(final int port) {
            final Responder responder = new SpecificResponder(AvroSourceProtocol.class, this);
            nettyServer = new NettyServer(responder, new InetSocketAddress(HOSTNAME, port));
            nettyServer.start();
        }

        public void stop() {
            nettyServer.close();
        }

        public Event poll() {

            AvroFlumeEvent avroEvent = null;
            try {
                avroEvent = eventQueue.poll(30000, TimeUnit.MILLISECONDS);
            } catch (final InterruptedException ie) {
                // Ignore the exception.
            }
            if (avroEvent != null) {
                return EventBuilder.withBody(avroEvent.getBody().array(), toStringMap(avroEvent.getHeaders()));
            }
            System.out.println("No Event returned");
            return null;
        }

        @Override
        public Status append(final AvroFlumeEvent event) throws AvroRemoteException {
            eventQueue.add(event);
            //System.out.println("Received event " + event.getHeaders().get(new org.apache.avro.util.Utf8(FlumeEvent.GUID)));
            return Status.OK;
        }

        @Override
        public Status appendBatch(final List<AvroFlumeEvent> events) throws AvroRemoteException {
            Preconditions.checkState(eventQueue.addAll(events));
            for (final AvroFlumeEvent event : events) {
                // System.out.println("Received event " + event.getHeaders().get(new org.apache.avro.util.Utf8(FlumeEvent.GUID)));
            }
            return Status.OK;
        }
    }

    private static Map<String, String> toStringMap(final Map<CharSequence, CharSequence> charSeqMap) {
        final Map<String, String> stringMap = new HashMap<>();
        for (final Map.Entry<CharSequence, CharSequence> entry : charSeqMap.entrySet()) {
            stringMap.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return stringMap;
    }
}