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
package org.apache.logging.log4j.core.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContextTestAccess;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.test.TestProperties;
import org.apache.logging.log4j.test.junit.Resources;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.apache.logging.log4j.test.junit.UsingTestProperties;
import org.apache.logging.log4j.util.Unbox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.ResourceLock;

@UsingStatusListener
@UsingTestProperties
@ResourceLock(value = Resources.THREAD_CONTEXT)
public abstract class AbstractAsyncThreadContextTestBase {

    private static final int LINE_COUNT = 130;

    private static TestProperties props;

    @BeforeAll
    public static void beforeClass() {
        props.setProperty("log4j2.is.webapp", false);
        props.setProperty("AsyncLogger.RingBufferSize", 128); // minimum ringbuffer size
        props.setProperty("AsyncLoggerConfig.RingBufferSize", 128); // minimum ringbuffer size
    }

    enum Mode {
        ALL_ASYNC,
        MIXED,
        BOTH_ALL_ASYNC_AND_MIXED;

        void initSelector() {
            final ContextSelector selector;
            if (this == ALL_ASYNC || this == BOTH_ALL_ASYNC_AND_MIXED) {
                selector = new AsyncLoggerContextSelector();
            } else {
                selector = new ClassLoaderContextSelector();
            }
            LogManager.setFactory(new Log4jContextFactory(selector));
        }

        void initConfigFile() {
            // NOTICE: PLEASE DON'T REFACTOR: keep "file" local variable for confirmation in debugger.
            final String file = this == ALL_ASYNC //
                    ? "AsyncLoggerThreadContextTest.xml" //
                    : "AsyncLoggerConfigThreadContextTest.xml";
            props.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, file);
        }
    }

    enum ContextImpl {
        WEBAPP,
        GARBAGE_FREE,
        COPY_ON_WRITE;

        void init() {
            final String PACKAGE = "org.apache.logging.log4j.spi.";
            props.setProperty("log4j2.threadContextMap", PACKAGE + implClassSimpleName());
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

    private void init(final ContextImpl contextImpl, final Mode asyncMode) {
        asyncMode.initSelector();
        asyncMode.initConfigFile();

        contextImpl.init();
        // Verify that we are using the requested context map
        if (contextImpl == ContextImpl.WEBAPP) {
            assertThat(ThreadContext.getThreadContextMap()).isNull();
        } else {
            assertThat(ThreadContext.getThreadContextMap())
                    .isNotNull()
                    .extracting(o -> o.getClass().getSimpleName())
                    .isEqualTo(contextImpl.implClassSimpleName());
        }
    }

    protected void testAsyncLogWritesToLog(final ContextImpl contextImpl, final Mode asyncMode, final Path loggingPath)
            throws Exception {
        final Path testLoggingPath = loggingPath.resolve(asyncMode.toString());
        props.setProperty("logging.path", testLoggingPath.toString());
        init(contextImpl, asyncMode);
        final Path[] files = new Path[] {
            testLoggingPath.resolve("AsyncLoggerTest.log"),
            testLoggingPath.resolve("SynchronousContextTest.log"),
            testLoggingPath.resolve("AsyncLoggerAndAsyncAppenderTest.log"),
            testLoggingPath.resolve("AsyncAppenderContextTest.log"),
        };

        ThreadContext.push("stackvalue");
        ThreadContext.put("KEY", "mapvalue");

        final Logger log = LogManager.getLogger("com.foo.Bar");
        final LoggerContext loggerContext = LogManager.getContext(false);
        final String loggerContextName = loggerContext.getClass().getSimpleName();
        RingBufferAdmin ring;
        if (loggerContext instanceof AsyncLoggerContext) {
            ring = ((AsyncLoggerContext) loggerContext).createRingBufferAdmin();
        } else {
            ring = ((AsyncLoggerConfig) ((org.apache.logging.log4j.core.Logger) log).get()).createRingBufferAdmin("");
        }

        for (int i = 0; i < LINE_COUNT; i++) {
            // buffer may be full
            if (i >= 128) {
                waitAtMost(500, TimeUnit.MILLISECONDS)
                        .pollDelay(10, TimeUnit.MILLISECONDS)
                        .until(() -> ring.getRemainingCapacity() > 0);
            }
            if ((i & 1) == 1) {
                ThreadContext.put("count", String.valueOf(i));
            } else {
                ThreadContext.remove("count");
            }
            log.info("{} {} {} i={}", contextImpl, contextMap(), loggerContextName, Unbox.box(i));
        }
        ThreadContext.pop();
        CoreLoggerContexts.stopLoggerContext(false, files[0].toFile()); // stop async thread

        checkResult(files[0], loggerContextName, contextImpl);
        if (asyncMode == Mode.MIXED || asyncMode == Mode.BOTH_ALL_ASYNC_AND_MIXED) {
            for (int i = 1; i < files.length; i++) {
                checkResult(files[i], loggerContextName, contextImpl);
            }
        }
        LogManager.shutdown();
        FileUtils.deleteDirectory(testLoggingPath.toFile());
    }

    private static String contextMap() {
        final ReadOnlyThreadContextMap impl = ThreadContext.getThreadContextMap();
        return impl == null
                ? ContextImpl.WEBAPP.implClassSimpleName()
                : impl.getClass().getSimpleName();
    }

    private void checkResult(final Path file, final String loggerContextName, final ContextImpl contextImpl)
            throws IOException {
        final String contextDesc = contextImpl + " " + contextImpl.implClassSimpleName() + " " + loggerContextName;
        try (final BufferedReader reader = Files.newBufferedReader(file)) {
            String expect;
            for (int i = 0; i < LINE_COUNT; i++) {
                final String line = reader.readLine();
                if ((i & 1) == 1) {
                    expect =
                            "INFO c.f.Bar mapvalue [stackvalue] {KEY=mapvalue, configProp=configValue, configProp2=configValue2, count="
                                    + i + "} " + contextDesc + " i=" + i;
                } else {
                    expect =
                            "INFO c.f.Bar mapvalue [stackvalue] {KEY=mapvalue, configProp=configValue, configProp2=configValue2} "
                                    + contextDesc + " i=" + i;
                }
                assertThat(line).as("Log file '%s'", file.getFileName()).isEqualTo(expect);
            }
            assertThat(reader.readLine()).as("Last line").isNull();
        }
    }
}
