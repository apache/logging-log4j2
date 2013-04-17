package org.apache.logging.log4j.core.async.perftest;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.LifeCycle;

import com.lmax.disruptor.collections.Histogram;

public class RunLogback implements IPerfTestRunner {

	@Override
	public void runThroughputTest(int lines, Histogram histogram) {
		long s1 = System.nanoTime();
		Logger logger = (Logger) LoggerFactory.getLogger(getClass());
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
		Logger logger = (Logger) LoggerFactory.getLogger(getClass());
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
		((LifeCycle) LoggerFactory.getILoggerFactory()).stop();
	}

	@Override
	public void log(String msg) {
		Logger logger = (Logger) LoggerFactory.getLogger(getClass());
		logger.info(msg);
	}
}
