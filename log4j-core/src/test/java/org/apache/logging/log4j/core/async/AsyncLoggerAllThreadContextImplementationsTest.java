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
package org.apache.logging.log4j.core.async;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContextAccess;
import org.apache.logging.log4j.ThreadContextTestAccess;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.util.Unbox;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class AsyncLoggerAllThreadContextImplementationsTest {

    final static int LINE_COUNT = 130;
    private final ContextImpl contextImpl;
    private final Mode asyncMode;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("AsyncLogger.RingBufferSize", "128"); // minimum ringbuffer size
        System.setProperty("AsyncLoggerConfig.RingBufferSize", "128"); // minimum ringbuffer size
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty("AsyncLogger.RingBufferSize");
        System.clearProperty("AsyncLoggerConfig.RingBufferSize");
        System.clearProperty(Constants.LOG4J_CONTEXT_SELECTOR);
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        System.clearProperty("log4j2.garbagefree.threadContextMap");
        System.clearProperty("log4j2.is.webapp");
        System.clearProperty("log4j2.threadContextMap");
    }

    static enum Mode {
        ALL_ASYNC, MIXED, BOTH_ALL_ASYNC_AND_MIXED;

        void initSelector() {
            if (this == ALL_ASYNC || this == BOTH_ALL_ASYNC_AND_MIXED) {
                System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,  AsyncLoggerContextSelector.class.getName());
            } else {
                System.clearProperty(Constants.LOG4J_CONTEXT_SELECTOR);
            }
        }

        void initConfigFile() {
            System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, this == ALL_ASYNC //
                    ? "AsyncLoggerThreadContextTest.xml" //
                    : "AsyncLoggerConfigThreadContextTest.xml");
        }
    }

    static enum ContextImpl {
        WEBAPP, GARBAGE_FREE, COPY_ON_WRITE;

        void init() {
            System.clearProperty("log4j2.threadContextMap");
            final String PACKAGE = "org.apache.logging.log4j.spi.";
            System.setProperty("log4j2.threadContextMap", PACKAGE + implClassSimpleName());
            ThreadContextTestAccess.init();
        }

        public String implClassSimpleName() {
            switch (this) {
                case WEBAPP:
                    return DefaultThreadContextMap.class.getSimpleName();
                case GARBAGE_FREE:
                    return "GarbageFreeSortedArrayThreadContextMap";
                case COPY_ON_WRITE:
                    return "CopyOnWriteSortedArrayThreadContextMap";
            }
            throw new IllegalStateException("Unknown state " + this);
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { ContextImpl.WEBAPP, Mode.ALL_ASYNC},
                { ContextImpl.WEBAPP, Mode.BOTH_ALL_ASYNC_AND_MIXED},
                { ContextImpl.GARBAGE_FREE, Mode.ALL_ASYNC},
                { ContextImpl.GARBAGE_FREE, Mode.BOTH_ALL_ASYNC_AND_MIXED},
                { ContextImpl.COPY_ON_WRITE, Mode.ALL_ASYNC},
                { ContextImpl.COPY_ON_WRITE, Mode.BOTH_ALL_ASYNC_AND_MIXED}
        });
    }

    public AsyncLoggerAllThreadContextImplementationsTest(ContextImpl contextImpl, Mode asyncMode) {
        this.contextImpl = contextImpl;
        this.asyncMode = asyncMode;

        asyncMode.initSelector();
        asyncMode.initConfigFile();

        contextImpl.init();
    }

    @Test
    public void testAsyncLogWritesToLog() throws Exception {
        final File[] files = new File[] {
                new File("target", "AsyncLoggerTest.log"), //
                new File("target", "SynchronousContextTest.log"), //
                new File("target", "AsyncLoggerAndAsyncAppenderTest.log"), //
                new File("target", "AsyncAppenderContextTest.log"), //
        };
        for (final File f : files) {
            f.delete();
        }

        ThreadContext.push("stackvalue");
        ThreadContext.put("KEY", "mapvalue");

        final Logger log = LogManager.getLogger("com.foo.Bar");
        final LoggerContext loggerContext = LogManager.getContext(false);
        final String loggerContextName = loggerContext.getClass().getSimpleName();
        final RingBufferAdmin ring = ((AsyncLoggerContext) loggerContext).createRingBufferAdmin();
        for (int i = 0; i < LINE_COUNT; i++) {
            while (i >= 128 && ring.getRemainingCapacity() == 0) { // buffer may be full
                Thread.sleep(1);
            }
            if ((i & 1) == 1) {
                ThreadContext.put("count", String.valueOf(i));
            } else {
                ThreadContext.remove("count");
            }
            final String contextmap = ThreadContextAccess.getThreadContextMap().getClass().getSimpleName();
            log.info("{} {} {} i={}", contextImpl, contextmap, loggerContextName, Unbox.box(i));
        }
        ThreadContext.pop();
        CoreLoggerContexts.stopLoggerContext(false, files[0]); // stop async thread

        checkResult(files[0], loggerContextName);
        if (asyncMode == Mode.MIXED || asyncMode == Mode.BOTH_ALL_ASYNC_AND_MIXED) {
            for (int i = 1; i < files.length; i++) {
                checkResult(files[i], loggerContextName);
            }
        }
        LogManager.shutdown();
    }

    private void checkResult(final File file, final String loggerContextName) throws IOException {
        final String contextDesc = contextImpl + " " + contextImpl.implClassSimpleName() + " " + loggerContextName;
        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String expect = null;
            for (int i = 0; i < LINE_COUNT; i++) {
                String line = reader.readLine();
                if ((i & 1) == 1) {
                    expect = "INFO c.f.Bar mapvalue [stackvalue] {KEY=mapvalue, configProp=configValue, configProp2=configValue2, count=" + i + "} "
                            + contextDesc + " i=" + i;
                } else {
                    expect = "INFO c.f.Bar mapvalue [stackvalue] {KEY=mapvalue, configProp=configValue, configProp2=configValue2} " + contextDesc + " i=" + i;
                }
                assertEquals(file.getName() + ": line " + i, expect, line);
            }
            final String noMoreLines = reader.readLine();
            assertNull("done", noMoreLines);
        } finally {
            file.delete();
        }
    }
}
