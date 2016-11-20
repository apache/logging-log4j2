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
package org.apache.logging.log4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.util.Strings;

import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;

/**
 * Utily methods for the GC-free logging tests.s.
 */
public class GcFreeLoggingTestUtil {

    public static void executeLogging(final String configurationFile,
            final Class<?> testClass) throws Exception {

        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("log4j2.enable.direct.encoders", "true");
        System.setProperty("log4j2.is.webapp", "false");
        System.setProperty("log4j.configurationFile", configurationFile);

        assertTrue("Constants.ENABLE_THREADLOCALS", Constants.ENABLE_THREADLOCALS);
        assertTrue("Constants.ENABLE_DIRECT_ENCODERS", Constants.ENABLE_DIRECT_ENCODERS);
        assertFalse("Constants.IS_WEB_APP", Constants.IS_WEB_APP);

        final MyCharSeq myCharSeq = new MyCharSeq();
        final Marker testGrandParent = MarkerManager.getMarker("testGrandParent");
        final Marker testParent = MarkerManager.getMarker("testParent").setParents(testGrandParent);
        final Marker test = MarkerManager.getMarker("test").setParents(testParent); // initial creation, value is cached

        // initialize LoggerContext etc.
        // This is not steady-state logging and will allocate objects.
        ThreadContext.put("aKey", "value1");
        ThreadContext.put("key2", "value2");

        final org.apache.logging.log4j.Logger logger = LogManager.getLogger(testClass.getName());
        logger.debug("debug not set");
        logger.fatal(test, "This message is logged to the console");
        logger.error("Sample error message");
        logger.error("Test parameterized message {}", "param");
        logger.error(new MapMessage().with("eventId", "Login")); // initialize GelfLayout's messageStringBuilder
        for (int i = 0; i < 256; i++) {
            logger.debug("ensure all ringbuffer slots have been used once"); // allocate MutableLogEvent.messageText
        }
        ThreadContext.remove("aKey");
        ThreadContext.remove("key2");

        // BlockingWaitStrategy uses ReentrantLock which allocates Node objects. Ignore this.
        final String[] exclude = new String[] {
                "java/util/concurrent/locks/AbstractQueuedSynchronizer$Node", //
                "com/google/monitoring/runtime/instrumentation/Sampler", //
        };
        final AtomicBoolean samplingEnabled = new AtomicBoolean(true);
        final Sampler sampler = new Sampler() {
            @Override
            public void sampleAllocation(final int count, final String desc, final Object newObj, final long size) {
                if (!samplingEnabled.get()) {
                    return;
                }
                for (int i = 0; i < exclude.length; i++) {
                    if (exclude[i].equals(desc)) {
                        return; // exclude
                    }
                }
                System.err.println("I just allocated the object " + newObj +
                        " of type " + desc + " whose size is " + size);
                if (count != -1) {
                    System.err.println("It's an array of size " + count);
                }

                // show a stack trace to see which line caused allocation
                new RuntimeException().printStackTrace();
            }
        };
        Thread.sleep(500);
        final MapMessage mapMessage = new MapMessage().with("eventId", "Login");
        AllocationRecorder.addSampler(sampler);

        // now do some steady-state logging

        ThreadContext.put("aKey", "value1");
        ThreadContext.put("key2", "value2");

        final int ITERATIONS = 5;
        for (int i = 0; i < ITERATIONS; i++) {
            logger.error(myCharSeq);
            logger.error(MarkerManager.getMarker("test"), myCharSeq);
            logger.error("Test message");
            logger.error("Test parameterized message {}", "param");
            logger.error("Test parameterized message {}{}", "param", "param2");
            logger.error("Test parameterized message {}{}{}", "param", "param2", "abc");
            logger.error(mapMessage); // LOG4J2-1683
            ThreadContext.remove("aKey");
            ThreadContext.put("aKey", "value1");
        }
        Thread.sleep(50);
        samplingEnabled.set(false); // reliably ignore all allocations from now on
        AllocationRecorder.removeSampler(sampler);
        Thread.sleep(100);
    }

    public static void runTest(final Class<?> cls) throws Exception {
        final String javaHome = System.getProperty("java.home");
        final String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        final String classpath = System.getProperty("java.class.path");
        final String javaagent = "-javaagent:" + agentJar();

        final File tempFile = File.createTempFile("allocations", ".txt");
        tempFile.deleteOnExit();

        final ProcessBuilder builder = new ProcessBuilder( //
                javaBin, javaagent, "-cp", classpath, cls.getName());
        builder.redirectError(ProcessBuilder.Redirect.to(tempFile));
        builder.redirectOutput(ProcessBuilder.Redirect.to(tempFile));
        final Process process = builder.start();
        process.waitFor();
        process.exitValue();

        final String text = new String(Files.readAllBytes(tempFile.toPath()));
        final List<String> lines = Files.readAllLines(tempFile.toPath(), Charset.defaultCharset());
        final String className = cls.getSimpleName();
        assertEquals(text, "FATAL o.a.l.l.c." + className + " [main] value1 {aKey=value1, key2=value2, prop1=value1, prop2=value2} This message is logged to the console",
                lines.get(0));

        for (int i = 1; i < lines.size(); i++) {
            final String line = lines.get(i);
            assertFalse(i + ": " + line + Strings.LINE_SEPARATOR + text, line.contains("allocated") || line.contains("array"));
        }
    }

    private static File agentJar() {
        final String name = AllocationRecorder.class.getName();
        final URL url = AllocationRecorder.class.getResource("/" + name.replace('.', '/').concat(".class"));
        if (url == null) {
            throw new IllegalStateException("Could not find url for " + name);
        }
        final String temp = url.toString();
        final String path = temp.substring("jar:file:".length(), temp.indexOf('!'));
        return new File(path);
    }

    public static class MyCharSeq implements CharSequence {
        final String seq = GcFreeLoggingTestUtil.class.toString();

        @Override
        public int length() {
            return seq.length();
        }

        @Override
        public char charAt(final int index) {
            return seq.charAt(index);
        }

        @Override
        public CharSequence subSequence(final int start, final int end) {
            return seq.subSequence(start, end);
        }

        @Override
        public String toString() {
            System.err.println("TEMP OBJECT CREATED!");
            throw new IllegalStateException("TEMP OBJECT CREATED!");
        }
    }
}
