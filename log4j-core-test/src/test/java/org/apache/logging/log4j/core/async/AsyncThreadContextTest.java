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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.ThreadContextTestAccess;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.test.TestProperties;
import org.apache.logging.log4j.test.junit.InitializesThreadContext;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Unbox;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("sleepy")
@SetTestProperty(
        key = Log4jPropertyKey.Constant.ASYNC_LOGGER_RING_BUFFER_SIZE,
        value = "128") // minimum ringbuffer size
@SetTestProperty(
        key = Log4jPropertyKey.Constant.ASYNC_CONFIG_RING_BUFFER_SIZE,
        value = "128") // minimum ringbuffer size
@InitializesThreadContext
public class AsyncThreadContextTest {

    private static final int LINE_COUNT = 130;

    private static TestProperties props;

    @TempLoggingDir
    private static Path loggingPath;

    enum Mode {
        ALL_ASYNC(AsyncLoggerContextSelector.class, "AsyncLoggerThreadContextTest.xml"),
        MIXED(ClassLoaderContextSelector.class, "AsyncLoggerConfigThreadContextTest.xml"),
        BOTH_ALL_ASYNC_AND_MIXED(AsyncLoggerContextSelector.class, "AsyncLoggerConfigThreadContextTest.xml");

        final Class<? extends ContextSelector> contextSelectorType;
        final URI configUri;

        Mode(final Class<? extends ContextSelector> contextSelectorType, final String file) {
            this.contextSelectorType = contextSelectorType;
            configUri = NetUtils.toURI(file);
        }
    }

    enum ContextImpl {
        WEBAPP,
        GARBAGE_FREE,
        COPY_ON_WRITE;

        void init() {
            System.clearProperty(LoggingSystemProperty.Constant.THREAD_CONTEXT_MAP_CLASS);
            final String PACKAGE = "org.apache.logging.log4j.spi.";
            System.setProperty(
                    LoggingSystemProperty.Constant.THREAD_CONTEXT_MAP_CLASS, PACKAGE + implClassSimpleName());
            PropertiesUtil.getProperties().reload();
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

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
        "COPY_ON_WRITE, MIXED",
        "WEBAPP, MIXED",
        "COPY_ON_WRITE, ALL_ASYNC",
        "COPY_ON_WRITE, BOTH_ALL_ASYNC_AND_MIXED",
        "WEBAPP, ALL_ASYNC",
        "WEBAPP, BOTH_ALL_ASYNC_AND_MIXED",
        "GARBAGE_FREE, ALL_ASYNC",
        "GARBAGE_FREE, BOTH_ALL_ASYNC_AND_MIXED",
    })
    public void testAsyncLogWritesToLog(final ContextImpl contextImpl, final Mode asyncMode) throws Exception {
        doTestAsyncLogWritesToLog(contextImpl, asyncMode, getClass(), loggingPath, props);
    }

    static void doTestAsyncLogWritesToLog(
            final ContextImpl contextImpl,
            final Mode asyncMode,
            final Class<?> testClass,
            final Path loggingPath,
            final TestProperties props)
            throws Exception {
        final Path testLoggingPath = loggingPath.resolve(contextImpl.toString()).resolve(asyncMode.toString());
        props.setProperty("logging.path", testLoggingPath.toString());
        final Log4jContextFactory factory = DI.builder()
                .addInitialBindingFrom(ContextSelector.KEY)
                .toFunction(instanceFactory -> instanceFactory.getFactory(asyncMode.contextSelectorType))
                .build()
                .getInstance(Log4jContextFactory.class);
        final String fqcn = testClass.getName();
        final ClassLoader classLoader = testClass.getClassLoader();
        final String name = contextImpl.toString() + ' ' + asyncMode;
        contextImpl.init();
        final LoggerContext context = factory.getContext(fqcn, classLoader, null, false, asyncMode.configUri, name);
        runTest(context, contextImpl, asyncMode, testLoggingPath);
    }

    private static void runTest(
            final LoggerContext context, final ContextImpl contextImpl, final Mode asyncMode, final Path loggingPath)
            throws Exception {
        final Path[] files = new Path[] {
            loggingPath.resolve("AsyncLoggerTest.log"),
            loggingPath.resolve("SynchronousContextTest.log"),
            loggingPath.resolve("AsyncLoggerAndAsyncAppenderTest.log"),
            loggingPath.resolve("AsyncAppenderContextTest.log"),
        };
        ThreadContext.push("stackvalue");
        ThreadContext.put("KEY", "mapvalue");

        final Logger log = context.getLogger("com.foo.Bar");
        final String loggerContextName = context.getClass().getSimpleName();
        RingBufferAdmin ring;
        if (context instanceof AsyncLoggerContext) {
            ring = ((AsyncLoggerContext) context).createRingBufferAdmin();
        } else {
            ring = ((AsyncLoggerConfig) log.get()).createRingBufferAdmin("");
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
        context.stop();
        CoreLoggerContexts.stopLoggerContext(files[0].toFile()); // stop async thread

        checkResult(files[0], loggerContextName, contextImpl);
        if (asyncMode == Mode.MIXED || asyncMode == Mode.BOTH_ALL_ASYNC_AND_MIXED) {
            for (int i = 1; i < files.length; i++) {
                checkResult(files[i], loggerContextName, contextImpl);
            }
        }
    }

    private static String contextMap() {
        final ReadOnlyThreadContextMap impl = ThreadContext.getThreadContextMap();
        return impl == null
                ? ContextImpl.WEBAPP.implClassSimpleName()
                : impl.getClass().getSimpleName();
    }

    private static void checkResult(final Path file, final String loggerContextName, final ContextImpl contextImpl)
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
