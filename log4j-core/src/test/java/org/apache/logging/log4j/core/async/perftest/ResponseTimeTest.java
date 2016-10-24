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
package org.apache.logging.log4j.core.async.perftest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.HdrHistogram.Histogram;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.async.DefaultAsyncQueueFullPolicy;
import org.apache.logging.log4j.core.async.EventRoute;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Loader;

/**
 * Latency test showing both service time and response time.
 * <p>Service time = time to perform the desired operation, response time = service time + queueing time.</p>
 *
 * <h4>Gil's DOs and DONTs for latency testing</h4>
 * <p>
 * <a href="https://groups.google.com/d/msg/mechanical-sympathy/0gaBXxFm4hE/O9QomwHIJAAJ">https://groups.google.com/d/msg/mechanical-sympathy/0gaBXxFm4hE/O9QomwHIJAAJ</a>
 * </p>
 * If you are looking at the set of "stacks" below (all of which are queues/transports),
 * I would strongly encourage you to avoid repeating the mistakes of testing methodologies
 * that focus entirely on max achievable throughput and then report some (usually bogus)
 * latency stats at those max throughout modes.
 * The tech empower numbers are a classic example of this in play, and while they do provide some basis
 * for comparing a small aspect of behavior (what I call the "how fast can this thing drive off a cliff"
 * comparison, or "peddle to the metal" testing), those results are not very useful for comparing load
 * carrying capacities for anything that actually needs to maintain some form of responsiveness SLA or
 * latency spectrum requirements.
 * <p>
 * Rules of thumb I'd start with (some simple DOs and DON'Ts):
 * </p>
 * <ol>
 * <li>DO measure max achievable throughput, but DON'T get focused on it as the main or single axis of measurement /
 * comparison.
 * </li>
 * <li>DO measure response time / latency behaviors across a spectrum of attempted load levels (e.g. at attempted loads
 * between 2% to 100%+ of max established thoughout).</li>
 * <li>DO measure the response time / latency spectrum for each tested load (even for max throughout, for which response
 * time should linearly grow with test length, or the test is wrong). HdrHistogram is one good way to capture this
 * information.</li>
 * <li>DO make sure you are measuring response time correctly and labeling it right. If you also measure and report
 * service time, label it as such (don't call it "latency").</li>
 * <li>DO compare response time / latency spectrum at given loads.</li>
 * <li>DO [repeatedly] sanity check and calibrate the benchmark setup to verify that it produces expected results for
 * known forced scenarios. E.g. forced pauses of known size via ^Z or SIGSTOP/SIGCONT should produce expected response
 * time percentile levels. Attempting to load at &gt;100% than achieved throughput should result in response time / latency
 * measurements that grow with benchmark run length, while service time (if measured) should remain fairly flat well
 * past saturation.</li>
 * <li>DON'T use or report standard deviation for latency. Ever. Except if you mean it as a joke.</li>
 * <li>DON'T use average latency as a way to compare things with one another. [use median or 90%'ile instead, if what
 * you want to compare is "common case" latencies]. Consider not reporting avg. at all.</li>
 * <li>DON'T compare results of different setups or loads from short runs (&lt; 20-30 minutes).</li>
 * <li>DON'T include process warmup behavior (e.g. 1st minute and 1st 50K messages) in compared or reported
 * results.</li>
 * </ol>
 * <p.
 * See  <a href="https://groups.google.com/d/msg/mechanical-sympathy/0gaBXxFm4hE/O9QomwHIJAAJ">https://groups.google.com/d/msg/mechanical-sympathy/0gaBXxFm4hE/O9QomwHIJAAJ</a>
 * for some concrete visual examples.
 */
// RUN
// java -XX:+UnlockDiagnosticVMOptions -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution
// -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCApplicationStoppedTime -XX:GuaranteedSafepointInterval=500000
// -XX:CompileCommand=dontinline,org.apache.logging.log4j.core.async.perftest.NoOpIdleStrategy::idle
// -cp HdrHistogram-2.1.8.jar:disruptor-3.3.4.jar:log4j-api-2.6-SNAPSHOT.jar:log4j-core-2.6-SNAPSHOT.jar:log4j-core-2.6-SNAPSHOT-tests.jar
// -DAsyncLogger.WaitStrategy=busyspin -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
// -Dlog4j2.enable.threadlocals=true -Dlog4j2.enable.direct.encoders=true
//  -Xms1G -Xmx1G org.apache.logging.log4j.core.async.perftest.ResponseTimeTest 1 100000
//
// RUN recording in Java Flight Recorder:
// %JAVA_HOME%\bin\java -XX:+UnlockCommercialFeatures -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -XX:+FlightRecorder -XX:StartFlightRecording=duration=10m,filename=replayStats-2.6-latency.jfr -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCApplicationStoppedTime -XX:CompileCommand=dontinline,org.apache.logging.log4j.core.async.perftest.NoOpIdleStrategy::idle -DAsyncLogger.WaitStrategy=yield  -Dorg.apache.logging.log4j.simplelog.StatusLogger.level=TRACE -cp .;HdrHistogram-2.1.8.jar;disruptor-3.3.4.jar;log4j-api-2.6-SNAPSHOT.jar;log4j-core-2.6-SNAPSHOT.jar;log4j-core-2.6-SNAPSHOT-tests.jar org.apache.logging.log4j.core.async.perftest.ResponseTimeTest 1 50000
public class ResponseTimeTest {
    private static final String LATENCY_MSG = new String(new char[64]);

    public static void main(final String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Please specify thread count, target throughput (msg/sec) " +
                    "and logger library (Log4j1, Log4j2, Logback, JUL)");
            return;
        }
        final int threadCount = Integer.parseInt(args[0]);
        final double loadMessagesPerSec = Double.parseDouble(args[1]);
        final String loggerLib = args.length > 2 ? args[2] : "Log4j2";

        // print to console if ringbuffer is full
        System.setProperty("log4j2.AsyncQueueFullPolicy", PrintingAsyncQueueFullPolicy.class.getName());
        System.setProperty("AsyncLogger.RingBufferSize", String.valueOf(256 * 1024));
        //System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());
        //System.setProperty("log4j.configurationFile", "perf3PlainNoLoc.xml");
        if (System.getProperty("AsyncLogger.WaitStrategy") == null) {
            System.setProperty("AsyncLogger.WaitStrategy", "Yield");
        }
        //for (Object key : System.getProperties().keySet()) {
        //    System.out.println(key + "=" + System.getProperty((String) key));
        //}

        // initialize the logger
        final String wrapper = loggerLib.startsWith("Run") ? loggerLib : "Run" + loggerLib;
        final String loggerWrapperClass = "org.apache.logging.log4j.core.async.perftest." + wrapper;
        final IPerfTestRunner logger = Loader.newCheckedInstanceOf(loggerWrapperClass, IPerfTestRunner.class);
        logger.log("Starting..."); // ensure initialized
        Thread.sleep(100);

        final int requiredProcessors = threadCount + 1 + 1; // producers + 1 consumer + 1 for OS
        final IdleStrategy idleStrategy = Runtime.getRuntime().availableProcessors() > requiredProcessors
                ? new NoOpIdleStrategy()
                : new YieldIdleStrategy();

        System.out.printf("%s: %d threads, load is %,f msg/sec, using %s%n", loggerLib, threadCount,
                loadMessagesPerSec, idleStrategy.getClass().getSimpleName());

        // Warmup: run as many iterations of 50,000 calls to logger.log as we can in 1 minute
        final long WARMUP_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(1);
        final List<Histogram> warmupServiceTmHistograms = new ArrayList<>(threadCount);
        final List<Histogram> warmupResponseTmHistograms = new ArrayList<>(threadCount);

        final int WARMUP_COUNT = 50000 / threadCount;
        runLatencyTest(logger, WARMUP_DURATION_MILLIS, WARMUP_COUNT, loadMessagesPerSec, idleStrategy,
                warmupServiceTmHistograms, warmupResponseTmHistograms, threadCount);
        System.out.println("-----------------Warmup done. load=" + loadMessagesPerSec);
        if (!Constants.ENABLE_DIRECT_ENCODERS || !Constants.ENABLE_THREADLOCALS) {
            //System.gc();
            //Thread.sleep(5000);
        }
        System.out.println("-----------------Starting measured run. load=" + loadMessagesPerSec);

        final long start = System.currentTimeMillis();
        final List<Histogram> serviceTmHistograms = new ArrayList<>(threadCount);
        final List<Histogram> responseTmHistograms = new ArrayList<>(threadCount);
        PrintingAsyncQueueFullPolicy.ringbufferFull.set(0);

        // Actual test: run as many iterations of 1,000,000 calls to logger.log as we can in 4 minutes.
        final long TEST_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(4);
        final int COUNT = (1000 * 1000) / threadCount;
        runLatencyTest(logger, TEST_DURATION_MILLIS, COUNT, loadMessagesPerSec, idleStrategy, serviceTmHistograms,
                responseTmHistograms, threadCount);
        logger.shutdown();
        final long end = System.currentTimeMillis();

        // ... and report the results
        final Histogram resultServiceTm = createResultHistogram(serviceTmHistograms, start, end);
        resultServiceTm.outputPercentileDistribution(System.out, 1000.0);
        writeToFile("s", resultServiceTm, (int) (loadMessagesPerSec / 1000), 1000.0);

        final Histogram resultResponseTm = createResultHistogram(responseTmHistograms, start, end);
        resultResponseTm.outputPercentileDistribution(System.out, 1000.0);
        writeToFile("r", resultResponseTm, (int) (loadMessagesPerSec / 1000), 1000.0);

        System.out.printf("%n%s: %d threads, load %,f msg/sec, ringbuffer full=%d%n", loggerLib, threadCount,
                loadMessagesPerSec, PrintingAsyncQueueFullPolicy.ringbufferFull.get());
        System.out.println("Test duration: " + (end - start) / 1000.0 + " seconds");
    }

    private static void writeToFile(final String suffix, final Histogram hist, final int thousandMsgPerSec,
            final double scale) throws IOException {
        try (PrintStream pout = new PrintStream(new FileOutputStream(thousandMsgPerSec + "k" + suffix))) {
            hist.outputPercentileDistribution(pout, scale);
        }
    }

    private static Histogram createResultHistogram(final List<Histogram> list, final long start, final long end) {
        final Histogram result = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
        result.setStartTimeStamp(start);
        result.setEndTimeStamp(end);
        for (final Histogram hist : list) {
            result.add(hist);
        }
        return result;
    }

    public static void runLatencyTest(final IPerfTestRunner logger, final long durationMillis, final int samples,
            final double loadMessagesPerSec, final IdleStrategy idleStrategy,
            final List<Histogram> serviceTmHistograms, final List<Histogram> responseTmHistograms,
            final int threadCount) throws InterruptedException {

        final Thread[] threads = new Thread[threadCount];
        final CountDownLatch LATCH = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final Histogram serviceTmHist = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
            final Histogram responseTmHist = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
            serviceTmHistograms.add(serviceTmHist);
            responseTmHistograms.add(responseTmHist);

            threads[i] = new Thread("latencytest-" + i) {
                @Override
                public void run() {
                    LATCH.countDown();
                    try {
                        LATCH.await(); // wait until all threads are ready to go
                    } catch (final InterruptedException e) {
                        interrupt();
                        return;
                    }
                    final long endTimeMillis = System.currentTimeMillis() + durationMillis;
                    do {
                        final Pacer pacer = new Pacer(loadMessagesPerSec, idleStrategy);
                        runLatencyTest(samples, logger, serviceTmHist, responseTmHist, pacer);
                    } while (System.currentTimeMillis() < endTimeMillis);
                }
            };
            threads[i].start();
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
    }

    private static void runLatencyTest(final int samples, final IPerfTestRunner logger, final Histogram serviceTmHist,
            final Histogram responseTmHist, final Pacer pacer) {

        for (int i = 0; i < samples; i++) {
            final long expectedStartTimeNanos = pacer.expectedNextOperationNanoTime();
            pacer.acquire(1);
            final long actualStartTime = System.nanoTime();
            logger.log(LATENCY_MSG);
            final long doneTime = System.nanoTime();
            serviceTmHist.recordValue(doneTime - actualStartTime);
            responseTmHist.recordValue(doneTime - expectedStartTimeNanos);
        }
    }

    public static class PrintingAsyncQueueFullPolicy extends DefaultAsyncQueueFullPolicy {
        static AtomicLong ringbufferFull = new AtomicLong();

        @Override
        public EventRoute getRoute(final long backgroundThreadId, final Level level) {
            ringbufferFull.incrementAndGet();
            System.out.print('!');
            return super.getRoute(backgroundThreadId, level);
        }
    }

    /**
     * Pacer determines the pace at which measurements are taken. Sample usage:
     * <p/>
     * <pre>
     * - each thread has a Pacer instance
     * - at start of test, call pacer.setInitialStartTime(System.nanoTime());
     * - loop:
     *   - store result of pacer.expectedNextOperationNanoTime() as expectedStartTime
     *   - pacer.acquire(1);
     *   - before the measured operation: store System.nanoTime() as actualStartTime
     *   - perform the measured operation
     *   - store System.nanoTime() as doneTime
     *   - serviceTimeHistogram.recordValue(doneTime - actualStartTime);
     *   - responseTimeHistogram.recordValue(doneTime - expectedStartTime);
     * </pre>
     * <p>
     * Borrowed with permission from Gil Tene's Cassandra stress test:
     * https://github.com/LatencyUtils/cassandra-stress2/blob/trunk/tools/stress/src/org/apache/cassandra/stress/StressAction.java#L374
     * </p>
     */
    static class Pacer {
        private long initialStartTime;
        private double throughputInUnitsPerNsec;
        private long unitsCompleted;

        private boolean caughtUp = true;
        private long catchUpStartTime;
        private long unitsCompletedAtCatchUpStart;
        private double catchUpThroughputInUnitsPerNsec;
        private double catchUpRateMultiple;
        private final IdleStrategy idleStrategy;

        public Pacer(final double unitsPerSec, final IdleStrategy idleStrategy) {
            this(unitsPerSec, 3.0, idleStrategy); // Default to catching up at 3x the set throughput
        }

        public Pacer(final double unitsPerSec, final double catchUpRateMultiple, final IdleStrategy idleStrategy) {
            this.idleStrategy = idleStrategy;
            setThroughout(unitsPerSec);
            setCatchupRateMultiple(catchUpRateMultiple);
            initialStartTime = System.nanoTime();
        }

        public void setInitialStartTime(final long initialStartTime) {
            this.initialStartTime = initialStartTime;
        }

        public void setThroughout(final double unitsPerSec) {
            throughputInUnitsPerNsec = unitsPerSec / 1000000000.0;
            catchUpThroughputInUnitsPerNsec = catchUpRateMultiple * throughputInUnitsPerNsec;
        }

        public void setCatchupRateMultiple(final double multiple) {
            catchUpRateMultiple = multiple;
            catchUpThroughputInUnitsPerNsec = catchUpRateMultiple * throughputInUnitsPerNsec;
        }

        /**
         * @return the time for the next operation
         */
        public long expectedNextOperationNanoTime() {
            return initialStartTime + (long) (unitsCompleted / throughputInUnitsPerNsec);
        }

        public long nsecToNextOperation() {

            final long now = System.nanoTime();

            long nextStartTime = expectedNextOperationNanoTime();

            boolean sendNow = true;

            if (nextStartTime > now) {
                // We are on pace. Indicate caught_up and don't send now.}
                caughtUp = true;
                sendNow = false;
            } else {
                // We are behind
                if (caughtUp) {
                    // This is the first fall-behind since we were last caught up
                    caughtUp = false;
                    catchUpStartTime = now;
                    unitsCompletedAtCatchUpStart = unitsCompleted;
                }

                // Figure out if it's time to send, per catch up throughput:
                final long unitsCompletedSinceCatchUpStart =
                        unitsCompleted - unitsCompletedAtCatchUpStart;

                nextStartTime = catchUpStartTime +
                        (long) (unitsCompletedSinceCatchUpStart / catchUpThroughputInUnitsPerNsec);

                if (nextStartTime > now) {
                    // Not yet time to send, even at catch-up throughout:
                    sendNow = false;
                }
            }

            return sendNow ? 0 : (nextStartTime - now);
        }

        /**
         * Will wait for next operation time. After this the expectedNextOperationNanoTime() will move forward.
         *
         * @param unitCount
         */
        public void acquire(final long unitCount) {
            final long nsecToNextOperation = nsecToNextOperation();
            if (nsecToNextOperation > 0) {
                sleepNs(nsecToNextOperation);
            }
            unitsCompleted += unitCount;
        }

        private void sleepNs(final long ns) {
            long now = System.nanoTime();
            final long deadline = now + ns;
            while ((now = System.nanoTime()) < deadline) {
                idleStrategy.idle();
            }
        }
    }
}
