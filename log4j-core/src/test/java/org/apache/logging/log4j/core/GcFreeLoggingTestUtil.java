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

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.util.Constants;

import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;

import static org.junit.Assert.*;

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

        MyCharSeq myCharSeq = new MyCharSeq();
        MarkerManager.getMarker("test"); // initial creation, value is cached

        // initialize LoggerContext etc.
        // This is not steady-state logging and will allocate objects.
        final org.apache.logging.log4j.Logger logger = LogManager.getLogger(testClass.getName());
        logger.debug("debug not set");
        logger.fatal("This message is logged to the console");
        logger.error("Sample error message");
        logger.error("Test parameterized message {}", "param");
        for (int i = 0; i < 128; i++) {
            logger.debug("ensure all ringbuffer slots have been used once"); // allocate MutableLogEvent.messageText
        }

        // BlockingWaitStrategy uses ReentrantLock which allocates Node objects. Ignore this.
        final String[] exclude = new String[] {
                "java/util/concurrent/locks/AbstractQueuedSynchronizer$Node", //
                "com/google/monitoring/runtime/instrumentation/Sampler", //
        };
        final AtomicBoolean samplingEnabled = new AtomicBoolean(true);
        final Sampler sampler = new Sampler() {
            @Override
            public void sampleAllocation(int count, String desc, Object newObj, long size) {
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
        AllocationRecorder.addSampler(sampler);

        // now do some steady-state logging
        final int ITERATIONS = 5;
        for (int i = 0; i < ITERATIONS; i++) {
            logger.error(myCharSeq);
            logger.error(MarkerManager.getMarker("test"), myCharSeq);
            logger.error("Test message");
            logger.error("Test parameterized message {}", "param");
            logger.error("Test parameterized message {}{}", "param", "param2");
            logger.error("Test parameterized message {}{}{}", "param", "param2", "abc");
        }
        Thread.sleep(50);
        samplingEnabled.set(false); // reliably ignore all allocations from now on
        AllocationRecorder.removeSampler(sampler);
        Thread.sleep(100);
    }

    public static void runTest(Class<?> cls) throws Exception {
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
        assertEquals(text, "FATAL o.a.l.l.c." + className + " [main]  This message is logged to the console",
                lines.get(0));

        final String LINESEP = System.getProperty("line.separator");
        for (int i = 1; i < lines.size(); i++) {
            final String line = lines.get(i);
            assertFalse(i + ": " + line + LINESEP + text, line.contains("allocated") || line.contains("array"));
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
