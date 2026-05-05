/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.osgi.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.linkBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import com.lmax.disruptor.ExceptionHandler;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContext;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junitpioneer.jupiter.Issue;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class DisruptorTest {

    private static final int MESSAGE_COUNT = 128;

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return options(
                linkBundle("org.apache.logging.log4j.api"),
                linkBundle("org.apache.logging.log4j.core"),
                linkBundle("com.lmax.disruptor"),
                // required by Pax Exam's logging
                linkBundle("org.objectweb.asm"),
                linkBundle("org.objectweb.asm.commons"),
                linkBundle("org.objectweb.asm.tree"),
                linkBundle("org.objectweb.asm.tree.analysis"),
                linkBundle("org.objectweb.asm.util"),
                linkBundle("org.apache.aries.spifly.dynamic.bundle").startLevel(2),
                linkBundle("slf4j.api"),
                linkBundle("ch.qos.logback.classic"),
                linkBundle("ch.qos.logback.core"),
                junitBundles());
    }

    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/3706")
    public void testDisruptorLog() throws IOException {
        ClassLoader threadContextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = createClassLoader();
        try {
            // Set the context classloader to an empty classloader, so attempts to use the TCCL will not find any
            // classes.
            Thread.currentThread().setContextClassLoader(classLoader);
            // Logger context
            LoggerContext context = getLoggerContext();
            assertTrue("LoggerContext is an instance of AsyncLoggerContext", context instanceof AsyncLoggerContext);
            final CustomConfiguration custom = (CustomConfiguration) context.getConfiguration();
            // Logging
            final Logger logger = LogManager.getLogger(getClass());
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                logger.info("Hello OSGI from Log4j2! {}", i);
            }

            context.stop();
            assertEquals(MESSAGE_COUNT, custom.getEvents().size());
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                final LogEvent event = custom.getEvents().get(i);
                assertEquals(
                        "Message nr " + i,
                        "Hello OSGI from Log4j2! " + i,
                        event.getMessage().getFormattedMessage());
                assertEquals(Level.INFO, event.getLevel());
            }
            custom.clearEvents();
            assertNull("Asynchronous exception", TestExceptionHandler.exception.get());
        } finally {
            Thread.currentThread().setContextClassLoader(threadContextClassLoader);
        }
    }

    private static ClassLoader createClassLoader() {
        // We want a classloader capable only of loading TestExceptionHandler.
        // This is needed to detect exceptions thrown by the asynchronous thread.
        return new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(TestExceptionHandler.class.getName())) {
                    return TestExceptionHandler.class;
                }
                throw new ClassNotFoundException(name);
            }

            @Override
            public URL getResource(String name) {
                return null; // No resources available.
            }
        };
    }

    private static LoggerContext getLoggerContext() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        assertEquals("AsyncDefault", ctx.getName());
        return ctx;
    }

    public static class TestExceptionHandler implements ExceptionHandler<RingBufferLogEvent> {

        private static final AtomicReference<Throwable> exception = new AtomicReference<>();

        @Override
        public void handleEventException(Throwable ex, long sequence, RingBufferLogEvent event) {
            setException(ex);
        }

        @Override
        public void handleOnStartException(Throwable ex) {
            setException(ex);
        }

        @Override
        public void handleOnShutdownException(Throwable ex) {
            setException(ex);
        }

        private static void setException(Throwable ex) {
            exception.compareAndSet(null, ex);
        }
    }
}
