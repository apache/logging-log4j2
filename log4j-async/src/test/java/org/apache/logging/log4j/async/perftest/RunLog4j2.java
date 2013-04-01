package org.apache.logging.log4j.async.perftest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;

import com.lmax.disruptor.collections.Histogram;

public class RunLog4j2 implements IPerfTestRunner {

    @Override
    public void runThroughputTest(int lines, Histogram histogram) {
        long s1 = System.nanoTime();
        Logger logger = LogManager.getLogger(getClass());
        for (int j = 0; j < lines; j++) {
            logger.info(THROUGHPUT_MSG);
        }
        long s2 = System.nanoTime();
        long opsPerSec = (1000L * 1000L * 1000L * lines) / (s2 - s1);
        histogram.addObservation(opsPerSec);
    }


    @Override
    public void runLatencyTest(int samples, Histogram histogram,
            long nanoTimeCost, int threadCount) {
        Logger logger = LogManager.getLogger(getClass());
        for (int i = 0; i < samples; i++) {
            long s1 = System.nanoTime();
            logger.info(LATENCY_MSG);
            long s2 = System.nanoTime();
            long value = s2 - s1 - nanoTimeCost;
            if (value > 0) {
                histogram.addObservation(value);
            }
            // wait 1 microsec
            final long PAUSE_NANOS = 10000 * threadCount;
            long pauseStart = System.nanoTime();
            while (PAUSE_NANOS > (System.nanoTime() - pauseStart)) {
                // busy spin
            }
        }
    }


    @Override
    public void shutdown() {
        ((LifeCycle) LogManager.getContext()).stop(); // stop async thread
    }


    @Override
    public void log(String finalMessage) {
        Logger logger = LogManager.getLogger(getClass());
        logger.info(finalMessage);
    }
}
