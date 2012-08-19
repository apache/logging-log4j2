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

import org.apache.flume.Channel;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 *
 */
public class FlumeEmbeddedAppenderTest {
    private static final String CONFIG = "embedded.xml";
    private static LoggerContext ctx;

    private static final int testServerPort = 12345;

    private AvroSource primarySource;
    private AvroSource altSource;
    private Channel channel;

    private String testPort;
    private String altPort;
    private int counter;

    @BeforeClass
    public static void setupClass() {
        // System.setProperty(DefaultConfiguration.DEFAULT_LEVEL, Level.DEBUG.toString());
    }

    @AfterClass
    public static void cleanupClass() {
        StatusLogger.getLogger().reset();
    }

    @Before
    public void setUp() throws Exception {
        primarySource = new AvroSource();
        primarySource.setName("Primary");
        altSource = new AvroSource();
        altSource.setName("Alternate");
        channel = new MemoryChannel();
        channel.setName("Memory");
        ++counter;

        Configurables.configure(channel, new Context());

        /*
        * Clear out all other appenders associated with this logger to ensure we're
        * only hitting the Avro appender.
        */
        Context context = new Context();
        testPort = String.valueOf(testServerPort);
        context.put("port", testPort);
        context.put("bind", "localhost");
        Configurables.configure(primarySource, context);

        context = new Context();
        altPort = String.valueOf(testServerPort + 1);
        context.put("port", altPort);
        context.put("bind", "localhost");
        Configurables.configure(altSource, context);

        List<Channel> channels = new ArrayList<Channel>();
        channels.add(channel);

        ChannelSelector cs = new ReplicatingChannelSelector();
        cs.setChannels(channels);

        primarySource.setChannelProcessor(new ChannelProcessor(cs));
        altSource.setChannelProcessor(new ChannelProcessor(cs));

        primarySource.start();
        altSource.start();

    	  Assert.assertTrue("Reached start or error", LifecycleController.waitForOneOf(
            primarySource, LifecycleState.START_OR_ERROR));
        Assert.assertEquals("Server is started", LifecycleState.START, primarySource.getLifecycleState());
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();
    }

    @After
    public void teardown() throws Exception {
        System.clearProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        primarySource.stop();
        altSource.stop();
	      Assert.assertTrue("Reached stop or error",
	           LifecycleController.waitForOneOf(primarySource, LifecycleState.STOP_OR_ERROR));
	      Assert.assertEquals("Server is stopped", LifecycleState.STOP,
            primarySource.getLifecycleState());
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> names = server.queryNames(new ObjectName("org.apache.flume.*:*"), null);
        for (ObjectName name : names) {
            try {
                server.unregisterMBean(name);
            } catch (Exception ex) {
                System.out.println("Unable to unregister " + name.toString());
            }
        }
    }

    @Test
    public void testLog4Event() throws InterruptedException, IOException {

        StructuredDataMessage msg = new StructuredDataMessage("Test", "Test Message", "Test");
        EventLogger.logEvent(msg);

        Transaction transaction = channel.getTransaction();
        transaction.begin();

        Event event = channel.take();
   	    Assert.assertNotNull(event);
        String body = getBody(event);
  	    Assert.assertTrue("Channel contained event, but not expected message. Received: " + body,
            body.endsWith("Test Message"));
	      transaction.commit();
	      transaction.close();

	      primarySource.stop();
    }

    @Test
    public void testMultiple() throws InterruptedException, IOException {

        for (int i = 0; i < 10; ++i) {
            StructuredDataMessage msg = new StructuredDataMessage("Test", "Test Message " + i, "Test");
            EventLogger.logEvent(msg);
        }
        for (int i = 0; i < 10; ++i) {
            Transaction transaction = channel.getTransaction();
            transaction.begin();

            Event event = channel.take();
            Assert.assertNotNull(event);
            String body = getBody(event);
            Assert.assertTrue("Channel contained event, but not expected message. Received: " + body,
                body.endsWith("Test Message " + i));
            transaction.commit();
            transaction.close();
        }

        primarySource.stop();
    }


    @Test
    public void testFailover() throws InterruptedException, IOException {
        Logger logger = LogManager.getLogger("testFailover");
        logger.debug("Starting testFailover");
        for (int i = 0; i < 10; ++i) {
            StructuredDataMessage msg = new StructuredDataMessage("Test", "Test Message " + i, "Test");
            EventLogger.logEvent(msg);
        }
        for (int i = 0; i < 10; ++i) {
            Transaction transaction = channel.getTransaction();
            transaction.begin();

            Event event = channel.take();
            Assert.assertNotNull(event);
            String body = getBody(event);
            Assert.assertTrue("Channel contained event, but not expected message. Received: " + body,
                body.endsWith("Test Message " + i));
            transaction.commit();
            transaction.close();
        }

        primarySource.stop();


        for (int i = 0; i < 10; ++i) {
            StructuredDataMessage msg = new StructuredDataMessage("Test", "Test Message " + i, "Test");
            EventLogger.logEvent(msg);
        }
        for (int i = 0; i < 10; ++i) {
            Transaction transaction = channel.getTransaction();
            transaction.begin();

            Event event = channel.take();
            Assert.assertNotNull(event);
            String body = getBody(event);
            Assert.assertTrue("Channel contained event, but not expected message. Received: " + body,
                body.endsWith("Test Message " + i));
            transaction.commit();
            transaction.close();
        }
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
