package org.apache.logging.log4j.core.async.perftest;

import com.lmax.disruptor.collections.Histogram;

public interface IPerfTestRunner {
    static final String LINE100 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890!\"#$%&'()-=^~|\\@`[]{};:+*,.<>/?_123456";
    static final String THROUGHPUT_MSG = LINE100 + LINE100 + LINE100 + LINE100
            + LINE100;
    static final String LATENCY_MSG = "Short msg";

    void runThroughputTest(int lines, Histogram histogram);

    void runLatencyTest(int samples, Histogram histogram, long nanoTimeCost,
            int threadCount);
    void shutdown();
    void log(String finalMessage);
}
