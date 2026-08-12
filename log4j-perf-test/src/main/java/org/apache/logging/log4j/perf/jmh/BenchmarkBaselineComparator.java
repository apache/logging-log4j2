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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;

/**
 * Compares two JMH JSON result files and flags benchmarks exceeding a throughput/latency degradation threshold.
 * <p>
 * Usage:
 * <pre>
 * java \
 *     -cp target/log4j-perf-test-*-uber.jar \
 *     org.apache.logging.log4j.perf.jmh.BenchmarkBaselineComparator \
 *     baseline.json current.json [output.adoc] [thresholdPercent]
 * </pre>
 */
public final class BenchmarkBaselineComparator {

    public static final BigDecimal DEFAULT_DEGRADATION_THRESHOLD_PERCENT = BigDecimal.valueOf(2);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private BenchmarkBaselineComparator() {}

    public static void main(final String[] args) throws Exception {
        if (args.length < 2 || args.length > 4) {
            throw new IllegalArgumentException(
                    "usage: <baselineJsonFile> <currentJsonFile> [outputAdocFile] [thresholdPercent]");
        }

        final File baselineFile = requireReadableFile(args[0], "baselineJsonFile");
        final File currentFile = requireReadableFile(args[1], "currentJsonFile");
        final File outputFile = args.length >= 3 ? new File(args[2]) : null;
        final BigDecimal threshold = args.length == 4
                ? new BigDecimal(args[3])
                : DEFAULT_DEGRADATION_THRESHOLD_PERCENT;

        final ComparisonResult result = compare(baselineFile, currentFile, threshold);
        printSummary(System.out, result, threshold);

        if (outputFile != null) {
            writeComparisonReport(result, outputFile, threshold);
        }

        if (!result.allWithinThreshold()) {
            System.exit(1);
        }
    }

    public static ComparisonResult compare(
            final File baselineFile, final File currentFile, final BigDecimal thresholdPercent) throws IOException {
        Objects.requireNonNull(baselineFile, "baselineFile");
        Objects.requireNonNull(currentFile, "currentFile");
        Objects.requireNonNull(thresholdPercent, "thresholdPercent");

        final Map<String, BenchmarkMetric> baselineMetrics = readMetrics(baselineFile);
        final Map<String, BenchmarkMetric> currentMetrics = readMetrics(currentFile);

        final List<BenchmarkComparison> comparisons = new ArrayList<>();
        for (final Map.Entry<String, BenchmarkMetric> entry : baselineMetrics.entrySet()) {
            final String benchmark = entry.getKey();
            final BenchmarkMetric baselineMetric = entry.getValue();
            final BenchmarkMetric currentMetric = currentMetrics.get(benchmark);
            if (currentMetric == null) {
                comparisons.add(BenchmarkComparison.missing(benchmark, baselineMetric));
                continue;
            }
            if (!baselineMetric.mode.equals(currentMetric.mode)) {
                throw new IllegalArgumentException(String.format(
                        "benchmark mode mismatch for %s: baseline=%s current=%s",
                        benchmark, baselineMetric.mode, currentMetric.mode));
            }
            final BigDecimal degradation =
                    computeDegradationPercent(baselineMetric.mode, baselineMetric.score, currentMetric.score);
            comparisons.add(new BenchmarkComparison(
                    benchmark,
                    baselineMetric,
                    currentMetric,
                    degradation,
                    degradation.compareTo(thresholdPercent) > 0));
        }

        comparisons.sort(Comparator.comparing(comparison -> comparison.benchmark));
        return new ComparisonResult(comparisons);
    }

    public static void writeComparisonReport(
            final ComparisonResult result, final File outputAdocFile, final BigDecimal thresholdPercent)
            throws IOException {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(outputAdocFile, "outputAdocFile");
        touch(outputAdocFile);

        try (OutputStream outputStream = new FileOutputStream(outputAdocFile);
                PrintStream printStream = new PrintStream(outputStream, false, CHARSET.name())) {
            printStream.println("= JMH baseline comparison");
            printStream.println();
            printStream.format(
                    "Benchmark results compared against committed baseline fixtures. "
                            + "Degradation threshold: %s%%.%n",
                    thresholdPercent.stripTrailingZeros().toPlainString());
            printStream.println();
            printStream.println("[cols=\"4,>2,>2,>2,1\", options=\"header\"]");
            printStream.println("|===");
            printStream.println("|Benchmark");
            printStream.println("|Baseline");
            printStream.println("|Current");
            printStream.println("|Change");
            printStream.println("|Status");

            for (final BenchmarkComparison comparison : result.comparisons()) {
                if (comparison.currentMetric == null) {
                    printStream.format("|%s%n", shortBenchmarkName(comparison.benchmark));
                    printStream.format("|%s%n", formatScore(comparison.baselineMetric));
                    printStream.println("|—");
                    printStream.println("|—");
                    printStream.println("|MISSING");
                    continue;
                }

                printStream.format("|%s%n", shortBenchmarkName(comparison.benchmark));
                printStream.format("|%s%n", formatScore(comparison.baselineMetric));
                printStream.format("|%s%n", formatScore(comparison.currentMetric));
                printStream.format(
                        "|%.2f%%%n", comparison.degradationPercent.setScale(2, RoundingMode.HALF_UP).doubleValue());
                printStream.format("|%s%n", comparison.exceedsThreshold ? "FAIL" : "PASS");
            }

            printStream.println("|===");
            printStream.println();
            printStream.format(
                    "Summary: %d benchmark(s) compared, %d exceeding the %s%% threshold.%n",
                    result.comparedCount(),
                    result.exceedingThresholdCount(),
                    thresholdPercent.stripTrailingZeros().toPlainString());
        }
    }

    static BigDecimal computeDegradationPercent(
            final String mode, final BigDecimal baselineScore, final BigDecimal currentScore) {
        if (baselineScore.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (isHigherBetter(mode)) {
            return baselineScore
                    .subtract(currentScore)
                    .multiply(HUNDRED)
                    .divide(baselineScore, 4, RoundingMode.HALF_UP);
        }
        return currentScore
                .subtract(baselineScore)
                .multiply(HUNDRED)
                .divide(baselineScore, 4, RoundingMode.HALF_UP);
    }

    private static boolean isHigherBetter(final String mode) {
        return "thrpt".equals(mode) || "ss".equals(mode);
    }

    private static Map<String, BenchmarkMetric> readMetrics(final File file) throws IOException {
        final List<Object> jmhResult = readJsonArray(file);
        final Map<String, BenchmarkMetric> metrics = new LinkedHashMap<>();
        for (final Object entry : jmhResult) {
            final String benchmark = readStringAtPath(entry, "benchmark");
            final String mode = readStringAtPath(entry, "mode");
            final BigDecimal score = readBigDecimalAtPath(entry, "primaryMetric", "score");
            final String scoreUnit = readStringAtPath(entry, "primaryMetric", "scoreUnit");
            metrics.put(benchmark, new BenchmarkMetric(benchmark, mode, score, scoreUnit));
        }
        return metrics;
    }

    private static List<Object> readJsonArray(final File file) throws IOException {
        final byte[] jsonBytes = Files.readAllBytes(file.toPath());
        final String json = new String(jsonBytes, CHARSET);
        @SuppressWarnings("unchecked")
        final List<Object> result = (List<Object>) JsonReader.read(json);
        return result;
    }

    private static File requireReadableFile(final String path, final String label) {
        final File file = new File(path);
        if (!file.isFile()) {
            throw new IllegalArgumentException(label + " doesn't point to a regular file: " + file);
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException(label + " is not readable: " + file);
        }
        return file;
    }

    private static void printSummary(
            final PrintStream printStream, final ComparisonResult result, final BigDecimal thresholdPercent) {
        printStream.format(
                "Compared %d benchmark(s); %d exceed the %s%% degradation threshold.%n",
                result.comparedCount(),
                result.exceedingThresholdCount(),
                thresholdPercent.stripTrailingZeros().toPlainString());
        result.comparisons().stream()
                .filter(BenchmarkComparison::exceedsThreshold)
                .forEach(comparison -> printStream.format(
                        "FAIL %s: %.2f%% degradation (baseline=%s current=%s)%n",
                        shortBenchmarkName(comparison.benchmark),
                        comparison.degradationPercent.setScale(2, RoundingMode.HALF_UP).doubleValue(),
                        formatScore(comparison.baselineMetric),
                        formatScore(comparison.currentMetric)));
    }

    private static String formatScore(final BenchmarkMetric metric) {
        return metric.score.stripTrailingZeros().toPlainString() + " " + metric.scoreUnit;
    }

    private static String shortBenchmarkName(final String benchmark) {
        final int lastDot = benchmark.lastIndexOf('.');
        return lastDot >= 0 ? benchmark.substring(lastDot + 1) : benchmark;
    }

    private static void touch(final File file) throws IOException {
        final Path path = file.toPath();
        if (Files.exists(path)) {
            Files.setLastModifiedTime(path, java.nio.file.attribute.FileTime.from(java.time.Instant.now()));
        } else {
            Files.createFile(path);
        }
    }

    private static String readStringAtPath(final Object object, final String... path) {
        final Object value = readObjectAtPath(object, path);
        return value != null ? String.valueOf(value) : "";
    }

    private static Object readObjectAtPath(final Object object, final String... path) {
        Object lastObject = object;
        for (final String key : path) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> lastMap = (Map<String, Object>) lastObject;
            lastObject = lastMap.get(key);
        }
        return lastObject;
    }

    private static BigDecimal readBigDecimalAtPath(final Object object, final String... path) {
        final Object value = readObjectAtPath(object, path);
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("failed to convert value at path to BigDecimal: " + value);
        }
        final Number number = (Number) value;
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        } else if (number instanceof Integer) {
            return BigDecimal.valueOf(number.intValue());
        } else if (number instanceof Long) {
            return BigDecimal.valueOf(number.longValue());
        } else if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        } else if (number instanceof Double) {
            return BigDecimal.valueOf(number.doubleValue());
        } else if (number instanceof Float) {
            return BigDecimal.valueOf(number.floatValue());
        }
        return BigDecimal.valueOf(number.doubleValue());
    }

    public static final class ComparisonResult {

        private final List<BenchmarkComparison> comparisons;

        ComparisonResult(final List<BenchmarkComparison> comparisons) {
            this.comparisons = Collections.unmodifiableList(new ArrayList<>(comparisons));
        }

        public List<BenchmarkComparison> comparisons() {
            return comparisons;
        }

        public boolean allWithinThreshold() {
            return comparisons.stream().noneMatch(BenchmarkComparison::exceedsThreshold);
        }

        public int comparedCount() {
            return (int) comparisons.stream()
                    .filter(comparison -> comparison.currentMetric != null)
                    .count();
        }

        public int exceedingThresholdCount() {
            return (int) comparisons.stream()
                    .filter(BenchmarkComparison::exceedsThreshold)
                    .count();
        }
    }

    public static final class BenchmarkComparison {

        private final String benchmark;

        private final BenchmarkMetric baselineMetric;

        private final BenchmarkMetric currentMetric;

        private final BigDecimal degradationPercent;

        private final boolean exceedsThreshold;

        BenchmarkComparison(
                final String benchmark,
                final BenchmarkMetric baselineMetric,
                final BenchmarkMetric currentMetric,
                final BigDecimal degradationPercent,
                final boolean exceedsThreshold) {
            this.benchmark = benchmark;
            this.baselineMetric = baselineMetric;
            this.currentMetric = currentMetric;
            this.degradationPercent = degradationPercent;
            this.exceedsThreshold = exceedsThreshold;
        }

        static BenchmarkComparison missing(final String benchmark, final BenchmarkMetric baselineMetric) {
            return new BenchmarkComparison(benchmark, baselineMetric, null, HUNDRED, true);
        }

        public String benchmark() {
            return benchmark;
        }

        public BigDecimal degradationPercent() {
            return degradationPercent;
        }

        public boolean exceedsThreshold() {
            return exceedsThreshold;
        }
    }

    static final class BenchmarkMetric {

        private final String benchmark;

        private final String mode;

        private final BigDecimal score;

        private final String scoreUnit;

        BenchmarkMetric(final String benchmark, final String mode, final BigDecimal score, final String scoreUnit) {
            this.benchmark = benchmark;
            this.mode = mode;
            this.score = score;
            this.scoreUnit = scoreUnit;
        }
    }
}
