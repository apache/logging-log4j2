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
import java.nio.file.Files;

import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Verifies steady state logging is GC-free.
 *
 * @see <a href="https://github.com/google/allocation-instrumenter">https://github.com/google/allocation-instrumenter</a>
 */
public class GcFreeLoggingTest {

    @Test
    public void testNoAllocationDuringSteadyStateLogging() throws Throwable {
        if (!Constants.ENABLE_THREADLOCALS || !Constants.ENABLE_DIRECT_ENCODERS) {
            return;
        }
        final String javaHome = System.getProperty("java.home");
        final String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        final String classpath = System.getProperty("java.class.path");
        final String javaagent = "-javaagent:" + agentJar();

        final File tempFile = File.createTempFile("allocations", ".txt");
        tempFile.deleteOnExit();

        final ProcessBuilder builder = new ProcessBuilder( //
                javaBin, javaagent, "-cp", classpath, GcFreeLoggingTest.class.getName());
        builder.redirectError(ProcessBuilder.Redirect.to(tempFile));
        builder.redirectOutput(ProcessBuilder.Redirect.to(tempFile));
        final Process process = builder.start();
        process.waitFor();
        process.exitValue();

        final String output = new String(Files.readAllBytes(tempFile.toPath()));
        final String NEWLINE = System.getProperty("line.separator");
        assertEquals("FATAL o.a.l.l.c.GcFreeLoggingTest [main]  This message is logged to the console"
                + NEWLINE, output);
    }

    /**
     * This code runs in a separate process, instrumented with the Google Allocation Instrumenter.
     */
    public static void main(String[] args) throws Exception {
        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("log4j2.is.webapp", "false");
        System.setProperty("log4j.configurationFile", "gcFreeLogging.xml");
        System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());

        assertTrue("Constants.ENABLE_THREADLOCALS", Constants.ENABLE_THREADLOCALS);
        assertFalse("Constants.IS_WEB_APP", Constants.IS_WEB_APP);

        // initialize LoggerContext etc.
        // This is not steady-state logging and will allocate objects.
        final Logger logger = LogManager.getLogger(GcFreeLoggingTest.class.getName());
        logger.debug("debug not set");
        logger.fatal("This message is logged to the console");
        logger.error("Sample error message");
        logger.error("Test parameterized message {}", "param");

        // BlockingWaitStrategy uses ReentrantLock which allocates Node objects. Ignore this.
        final String[] exclude = new String[] {
                "java/util/concurrent/locks/AbstractQueuedSynchronizer$Node", //
                "com/google/monitoring/runtime/instrumentation/Sampler", //
        };
        final Sampler sampler = new Sampler() {
            public void sampleAllocation(int count, String desc, Object newObj, long size) {
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
            logger.error("Test message");
            logger.error("Test parameterized message {}", "param");
            logger.error("Test parameterized message {}{}", "param", "param2");
            logger.error("Test parameterized message {}{}{}", "param", "param2", "abc");
        }
        Thread.sleep(50);
        AllocationRecorder.removeSampler(sampler);
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
}
