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
package org.apache.logging.log4j.spi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.simple.SimpleLoggerContext;
import org.apache.logging.log4j.test.TestLogger;
import org.apache.logging.log4j.test.TestLoggerContext;
import org.apache.logging.log4j.test.TestLoggerContextFactory;
import org.junit.jupiter.api.Test;

/**
 * Created by Pavel.Sivolobtchik@uxpsystems.com on 2016-10-19.
 */
public class LoggerAdapterTest {

    private static class RunnableThreadTest implements Runnable {
        private final AbstractLoggerAdapter<Logger> adapter;
        private final LoggerContext context;
        private final CountDownLatch doneSignal;
        private final int index;
        private Map<String, Logger> resultMap;

        private final CountDownLatch startSignal;

        public RunnableThreadTest(
                final int index,
                final TestLoggerAdapter adapter,
                final LoggerContext context,
                final CountDownLatch startSignal,
                final CountDownLatch doneSignal) {
            this.adapter = adapter;
            this.context = context;
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
            this.index = index;
        }

        public Map<String, Logger> getResultMap() {
            return resultMap;
        }

        @Override
        public void run() {
            try {
                startSignal.await();
                resultMap = adapter.getLoggersInContext(context);
                resultMap.put(String.valueOf(index), new TestLogger());
                doneSignal.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class TestLoggerAdapter extends AbstractLoggerAdapter<Logger> {

        @Override
        protected LoggerContext getContext() {
            return null;
        }

        @Override
        protected Logger newLogger(final String name, final LoggerContext context) {
            return null;
        }
    }

    private static class TestLoggerAdapter2 extends AbstractLoggerAdapter<Logger> {

        @Override
        protected Logger newLogger(final String name, final LoggerContext context) {
            return context.getLogger(name);
        }

        @Override
        protected LoggerContext getContext() {
            return null;
        }

        public LoggerContext getContext(final String fqcn) {
            for (LoggerContext lc : registry.keySet()) {
                final TestLoggerContext2 context = (TestLoggerContext2) lc;
                if (fqcn.equals(context.getName())) {
                    return context;
                }
            }
            final LoggerContext lc = new TestLoggerContext2(fqcn, this);
            registry.put(lc, new ConcurrentHashMap<String, Logger>());
            return lc;
        }
    }

    private static class TestLoggerContext2 extends TestLoggerContext {
        private final String name;
        private final LoggerContextShutdownAware listener;

        public TestLoggerContext2(final String name, final LoggerContextShutdownAware listener) {
            this.name = name;
            this.listener = listener;
        }

        public String getName() {
            return name;
        }

        public void shutdown() {
            listener.contextShutdown(this);
        }
    }

    @Test
    public void testCleanup() throws Exception {
        final LoggerContextFactory factory = new TestLoggerContextFactory();
        final TestLoggerAdapter2 adapter = new TestLoggerAdapter2();
        for (int i = 0; i < 5; ++i) {
            final LoggerContext lc = adapter.getContext(Integer.toString(i));
            lc.getLogger(Integer.toString(i));
        }
        assertEquals(5, adapter.registry.size(), "Expected 5 LoggerContexts");
        final Set<LoggerContext> contexts = new HashSet<>(adapter.registry.keySet());
        for (LoggerContext context : contexts) {
            ((TestLoggerContext2) context).shutdown();
        }
        assertEquals(0, adapter.registry.size(), "Expected 0 LoggerContexts");
    }

    /**
     * Testing synchronization in the getLoggersInContext() method
     */
    @Test
    public synchronized void testGetLoggersInContextSynch() throws Exception {
        final TestLoggerAdapter adapter = new TestLoggerAdapter();

        final int num = 500;

        final CountDownLatch startSignal = new CountDownLatch(1);
        final CountDownLatch doneSignal = new CountDownLatch(num);

        final RunnableThreadTest[] instances = new RunnableThreadTest[num];
        LoggerContext lastUsedContext = null;
        for (int i = 0; i < num; i++) {
            if (i % 2 == 0) {
                // every other time create a new context
                lastUsedContext = new SimpleLoggerContext();
            }
            final RunnableThreadTest runnable =
                    new RunnableThreadTest(i, adapter, lastUsedContext, startSignal, doneSignal);
            final Thread thread = new Thread(runnable);
            thread.start();
            instances[i] = runnable;
        }

        startSignal.countDown();
        doneSignal.await();

        for (int i = 0; i < num; i = i + 2) {
            // maps for the same context should be the same instance
            final Map<String, Logger> resultMap1 = instances[i].getResultMap();
            final Map<String, Logger> resultMap2 = instances[i + 1].getResultMap();
            assertSame(resultMap1, resultMap2, "not the same map for instances" + i + " and " + (i + 1) + ":");
            assertEquals(2, resultMap1.size());
        }
    }
}
