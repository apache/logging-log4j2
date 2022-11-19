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
import java.net.URI;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.ThreadContextTestAccess;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.jmx.RingBufferAdmin;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.LoggingSystemProperties;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.test.junit.CleanUpFiles;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Unbox;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("sleepy")
@SetSystemProperty(key = Log4jProperties.ASYNC_LOGGER_RING_BUFFER_SIZE, value = "128") // minimum ringbuffer size
@SetSystemProperty(key = Log4jProperties.ASYNC_CONFIG_RING_BUFFER_SIZE, value = "128") // minimum ringbuffer size
public class AsyncThreadContextTest {

    private final static int LINE_COUNT = 130;
    public static final File[] FILES = {
            new File("target", "AsyncLoggerTest.log"), //
            new File("target", "SynchronousContextTest.log"), //
            new File("target", "AsyncLoggerAndAsyncAppenderTest.log"), //
            new File("target", "AsyncAppenderContextTest.log"), //
    };

    @AfterAll
    public static void afterClass() {
        System.clearProperty(LoggingSystemProperties.THREAD_CONTEXT_GARBAGE_FREE_ENABLED);
        System.clearProperty(LoggingSystemProperties.THREAD_CONTEXT_MAP_CLASS);
    }

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
        WEBAPP, GARBAGE_FREE, COPY_ON_WRITE;

        void init() {
            System.clearProperty(LoggingSystemProperties.THREAD_CONTEXT_MAP_CLASS);
            final String PACKAGE = "org.apache.logging.log4j.spi.";
            System.setProperty(LoggingSystemProperties.THREAD_CONTEXT_MAP_CLASS, PACKAGE + implClassSimpleName());
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
    @CleanUpFiles({
            "target/AsyncLoggerTest.log",
            "target/SynchronousContextTest.log",
            "target/AsyncLoggerAndAsyncAppenderTest.log",
            "target/AsyncAppenderContextTest.log",
    })
    public void testAsyncLogWritesToLog(final ContextImpl contextImpl, final Mode asyncMode) throws Exception {
        doTestAsyncLogWritesToLog(contextImpl, asyncMode, getClass());
    }

    static void doTestAsyncLogWritesToLog(final ContextImpl contextImpl, final Mode asyncMode, final Class<?> testClass) throws Exception {
        final Injector injector = DI.createInjector();
        injector.registerBinding(ContextSelector.KEY, injector.getFactory(asyncMode.contextSelectorType));
        injector.init();
        final Log4jContextFactory factory = new Log4jContextFactory(injector);
        final String fqcn = testClass.getName();
        final ClassLoader classLoader = testClass.getClassLoader();
        final String name = contextImpl.toString() + ' ' + asyncMode;
        contextImpl.init();
        final LoggerContext context = factory.getContext(fqcn, classLoader, null, false, asyncMode.configUri, name);
        runTest(context, contextImpl, asyncMode);
    }

    private static void runTest(final LoggerContext context, final ContextImpl contextImpl, final Mode asyncMode) throws Exception {
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
            while (i >= 128 && ring.getRemainingCapacity() == 0) { // buffer may be full
                Thread.sleep(1);
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
        CoreLoggerContexts.stopLoggerContext(FILES[0]); // stop async thread

        checkResult(FILES[0], loggerContextName, contextImpl);
        if (asyncMode == Mode.MIXED || asyncMode == Mode.BOTH_ALL_ASYNC_AND_MIXED) {
            for (int i = 1; i < FILES.length; i++) {
                checkResult(FILES[i], loggerContextName, contextImpl);
            }
        }
    }

    private static String contextMap() {
        final ReadOnlyThreadContextMap impl = ThreadContext.getThreadContextMap();
        return impl == null ? ContextImpl.WEBAPP.implClassSimpleName() : impl.getClass().getSimpleName();
    }

    private static void checkResult(final File file, final String loggerContextName, final ContextImpl contextImpl) throws IOException {
        final String contextDesc = contextImpl + " " + contextImpl.implClassSimpleName() + " " + loggerContextName;
        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String expect;
            for (int i = 0; i < LINE_COUNT; i++) {
                final String line = reader.readLine();
                if ((i & 1) == 1) {
                    expect = "INFO c.f.Bar mapvalue [stackvalue] {KEY=mapvalue, configProp=configValue, configProp2=configValue2, count=" + i + "} "
                            + contextDesc + " i=" + i;
                } else {
                    expect = "INFO c.f.Bar mapvalue [stackvalue] {KEY=mapvalue, configProp=configValue, configProp2=configValue2} " + contextDesc + " i=" + i;
                }
                assertEquals(expect, line, file.getName() + ": line " + i);
            }
            final String noMoreLines = reader.readLine();
            assertNull(noMoreLines, "done");
        } finally {
            file.delete();
        }
    }
}
