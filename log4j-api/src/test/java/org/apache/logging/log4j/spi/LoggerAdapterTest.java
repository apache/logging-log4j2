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
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.simple.SimpleLoggerContext;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Created by Pavel.Sivolobtchik@uxpsystems.com on 2016-10-19.
 */
public class LoggerAdapterTest {

    private class RunnableThreadTest implements Runnable {
        private final AbstractLoggerAdapter<Logger> adapter;
        private final LoggerContext context;
        private final CountDownLatch doneSignal;
        private final int index;
        private Map<String, Logger> resultMap;

        private final CountDownLatch startSignal;

        public RunnableThreadTest(final int index, final TestLoggerAdapter adapter, final LoggerContext context,
                final CountDownLatch startSignal, final CountDownLatch doneSignal) {
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
            }
            catch (final Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static class TestLogger extends Logger {
        public TestLogger() {
            super("test", null);
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
                //every other time create a new context
                lastUsedContext = new SimpleLoggerContext();
            }
            final RunnableThreadTest runnable = new RunnableThreadTest(i, adapter, lastUsedContext, startSignal, doneSignal);
            final Thread thread = new Thread(runnable);
            thread.start();
            instances[i] = runnable;
        }

        startSignal.countDown();
        doneSignal.await();

        for (int i = 0; i < num; i = i + 2) {
            //maps for the same context should be the same instance
            final Map<String, Logger> resultMap1 = instances[i].getResultMap();
            final Map<String, Logger> resultMap2 = instances[i + 1].getResultMap();
            assertSame("not the same map for instances" + i + " and " + (i + 1) + ":", resultMap1, resultMap2);
            assertEquals(2, resultMap1.size());
        }
    }
}