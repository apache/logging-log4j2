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
package org.apache.logging.log4j.perf.jmh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BenchmarkBaselineComparatorTest {

    private static final BigDecimal THRESHOLD = BenchmarkBaselineComparator.DEFAULT_DEGRADATION_THRESHOLD_PERCENT;

    @Test
    void computeDegradationPercentForThroughput() {
        assertEquals(
                0,
                BenchmarkBaselineComparator.computeDegradationPercent(
                                "thrpt", BigDecimal.valueOf(1000), BigDecimal.valueOf(1000))
                        .compareTo(BigDecimal.ZERO));
        assertEquals(
                0,
                BenchmarkBaselineComparator.computeDegradationPercent(
                                "thrpt", BigDecimal.valueOf(1000), BigDecimal.valueOf(990))
                        .compareTo(BigDecimal.valueOf(1.0)));
    }

    @Test
    void computeDegradationPercentForAverageTime() {
        assertEquals(
                0,
                BenchmarkBaselineComparator.computeDegradationPercent(
                                "avgt", BigDecimal.valueOf(50), BigDecimal.valueOf(50.5))
                        .compareTo(BigDecimal.valueOf(1.0)));
    }

    @Test
    void compareWithinThreshold() throws Exception {
        final File baseline = resource("baseline/comparator-fixture-baseline.json");
        final File current = resource("baseline/comparator-fixture-current-pass.json");

        final BenchmarkBaselineComparator.ComparisonResult result =
                BenchmarkBaselineComparator.compare(baseline, current, THRESHOLD);

        assertTrue(result.allWithinThreshold());
        assertEquals(2, result.comparedCount());
        assertEquals(0, result.exceedingThresholdCount());
    }

    @Test
    void compareExceedsThreshold() throws Exception {
        final File baseline = resource("baseline/comparator-fixture-baseline.json");
        final File current = resource("baseline/comparator-fixture-current-fail.json");

        final BenchmarkBaselineComparator.ComparisonResult result =
                BenchmarkBaselineComparator.compare(baseline, current, THRESHOLD);

        assertFalse(result.allWithinThreshold());
        assertEquals(2, result.comparedCount());
        assertEquals(2, result.exceedingThresholdCount());
    }

    private static File resource(final String name) {
        final File file = new File(BenchmarkBaselineComparatorTest.class.getClassLoader()
                .getResource(name)
                .getFile());
        if (!file.isFile()) {
            throw new IllegalStateException("missing test resource: " + name);
        }
        return file;
    }
}
