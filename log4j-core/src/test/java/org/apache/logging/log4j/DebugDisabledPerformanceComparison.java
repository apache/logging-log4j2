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
package org.apache.logging.log4j;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.logging.log4j.categories.PerformanceTests;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.Profiler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 *
 */
@Category(PerformanceTests.class)
public class DebugDisabledPerformanceComparison {

    private final Logger logger = LogManager.getLogger(DebugDisabledPerformanceComparison.class.getName());
    private final org.slf4j.Logger logbacklogger = org.slf4j.LoggerFactory.getLogger(DebugDisabledPerformanceComparison.class);
    private final org.apache.log4j.Logger log4jlogger = org.apache.log4j.Logger.getLogger(DebugDisabledPerformanceComparison.class);


    // How many times should we try to log:
    private static final int COUNT = 10000000;
    private static final int PROFILE_COUNT = 500000;
    private static final int WARMUP = 1000;

    private static final String CONFIG = "log4j2-perf2.xml";
    private static final String LOGBACK_CONFIG = "logback-perf2.xml";
    private static final String LOG4J_CONFIG = "log4j12-perf2.xml";

    private static final String LOGBACK_CONF = "logback.configurationFile";
    private static final String LOG4J_CONF = "log4j.configuration";

    @BeforeClass
    public static void setupClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        System.setProperty(LOGBACK_CONF, LOGBACK_CONFIG);
        System.setProperty(LOG4J_CONF, LOG4J_CONFIG);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        System.clearProperty(LOGBACK_CONF);
        System.clearProperty(LOG4J_CONF);
    }

    @Test
    public void testPerformance() throws Exception {

        log4j(WARMUP);
        logback(WARMUP);
        log4j2(WARMUP);

        if (Profiler.isActive()) {
            System.out.println("Profiling Log4j 2.0");
            Profiler.start();
            final long result = log4j2(PROFILE_COUNT);
            Profiler.stop();
            System.out.println("###############################################");
            System.out.println("Log4j 2.0: " + result);
            System.out.println("###############################################");
        } else {
            System.out.println("Starting isDebugEnabled tests");
            System.out.println("Starting Log4j 2.0");
            long result3 = log4j2IsDebug(COUNT);
            System.out.println("Starting Log4j");
            long result1 = log4j2IsDebug(COUNT);
            System.out.println("Starting Logback");
            long result2 = logbackIsDebug(COUNT );

            System.out.println("###############################################");
            System.out.println("Log4j: " + result1);
            System.out.println("Logback: " + result2);
            System.out.println("Log4j 2.0: " + result3);
            System.out.println("###############################################");

            System.out.println("Starting logger.debug tests with String concatenation");
            System.out.println("Starting Log4j 2.0");
            result3 = log4j2(COUNT);
            System.out.println("Starting Log4j");
            result1 = log4j(COUNT);
            System.out.println("Starting Logback");
            result2 = logback(COUNT);

            System.out.println("###############################################");
            System.out.println("Log4j: " + result1);
            System.out.println("Logback: " + result2);
            System.out.println("Log4j 2.0: " + result3);
            System.out.println("###############################################");

            System.out.println("Starting logger.debug tests without String concatenation");
            System.out.println("Starting Log4j 2.0");
            result3 = log4j2Debug(COUNT);
            System.out.println("Starting Logback");
            result2 = logbackDebug(COUNT);

            System.out.println("###############################################");
            System.out.println("Log4j: Not supported");
            System.out.println("Logback: " + result2);
            System.out.println("Log4j 2.0: " + result3);
            System.out.println("###############################################");
        }
    }

    //@Test
    public void testRawPerformance() throws Exception {
        final OutputStream os = new FileOutputStream("target/testos.log", true);
        final long result1 = writeToStream(COUNT, os);
        os.close();
        final OutputStream bos = new BufferedOutputStream(new FileOutputStream("target/testbuffer.log", true));
        final long result2 = writeToStream(COUNT, bos);
        bos.close();
        final Writer w = new FileWriter("target/testwriter.log", true);
        final long result3 = writeToWriter(COUNT, w);
        w.close();
        final FileOutputStream cos = new FileOutputStream("target/testchannel.log", true);
        final FileChannel channel = cos.getChannel();
        final long result4 = writeToChannel(COUNT, channel);
        cos.close();
        System.out.println("###############################################");
        System.out.println("FileOutputStream: " + result1);
        System.out.println("BufferedOutputStream: " + result2);
        System.out.println("FileWriter: " + result3);
        System.out.println("FileChannel: " + result4);
        System.out.println("###############################################");
    }


    private long log4jIsDebug(final int loop) {
        final Integer j = new Integer(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            log4jlogger.isDebugEnabled();
        }
        return (System.nanoTime() - start) / loop;
    }

    private long logbackIsDebug(final int loop) {
        final Integer j = new Integer(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            logbacklogger.isDebugEnabled();
        }
        return (System.nanoTime() - start) / loop;
    }


    private long log4j2IsDebug(final int loop) {
        final Integer j = new Integer(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            logger.isDebugEnabled();
        }
        return (System.nanoTime() - start) / loop;
    }



    private long log4j(final int loop) {
        final Integer j = new Integer(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            log4jlogger.debug("SEE IF THIS IS LOGGED " + j + '.');
        }
        return (System.nanoTime() - start) / loop;
    }

    private long logback(final int loop) {
        final Integer j = new Integer(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            logbacklogger.debug("SEE IF THIS IS LOGGED " + j + '.');
        }
        return (System.nanoTime() - start) / loop;
    }

    private long logbackDebug(final int loop) {
        final Integer j = new Integer(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            logbacklogger.debug("SEE IF THIS IS LOGGED {} .", j);
        }
        return (System.nanoTime() - start) / loop;
    }


    private long log4j2(final int loop) {
        final Integer j = new Integer(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            logger.debug("SEE IF THIS IS LOGGED " + j + '.');
        }
        return (System.nanoTime() - start) / loop;
    }

     private long log4j2Debug(final int loop) {
        final Integer j = new Integer(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            logger.debug("SEE IF THIS IS LOGGED {} .", j);
        }
        return (System.nanoTime() - start) / loop;
    }


    private long writeToWriter(final int loop, final Writer w) throws Exception {
        final Integer j = new Integer(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            w.write("SEE IF THIS IS LOGGED " + j + '.');
        }
        return (System.nanoTime() - start) / loop;
    }

    private long writeToStream(final int loop, final OutputStream os) throws Exception {
        final Integer j = new Integer(2);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            os.write(getBytes("SEE IF THIS IS LOGGED " + j + '.'));
        }
        return (System.nanoTime() - start) / loop;
    }

    private long writeToChannel(final int loop, final FileChannel channel) throws Exception {
        final Integer j = new Integer(2);
        final ByteBuffer buf = ByteBuffer.allocateDirect(8*1024);
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            channel.write(getByteBuffer(buf, "SEE IF THIS IS LOGGED " + j + '.'));
        }
        return (System.nanoTime() - start) / loop;
    }

    private ByteBuffer getByteBuffer(final ByteBuffer buf, final String s) {
        buf.clear();
        buf.put(s.getBytes());
        buf.flip();
        return buf;
    }

    private byte[] getBytes(final String s) {
        return s.getBytes();
    }

}