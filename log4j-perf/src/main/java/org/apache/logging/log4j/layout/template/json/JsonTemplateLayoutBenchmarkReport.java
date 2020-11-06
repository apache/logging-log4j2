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
package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.util.Strings;

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
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class to summarize {@link JsonTemplateLayoutBenchmark} results in Asciidoctor.
 * <p>
 * Usage:
 * <pre>
 * java \
 *     -cp log4j-perf/target/benchmarks.jar \
 *     org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutBenchmarkReport \
 *     log4j-perf/target/JsonTemplateLayoutBenchmarkResult.json \
 *     log4j-perf/target/JsonTemplateLayoutBenchmarkReport.adoc
 * </pre>
 * @see JsonTemplateLayoutBenchmark on how to generate JMH result JSON file
 */
public enum JsonTemplateLayoutBenchmarkReport {;

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    public static void main(final String[] args) throws Exception {
        final CliArgs cliArgs = CliArgs.parseArgs(args);
        final JmhSetup jmhSetup = JmhSetup.ofJmhResult(cliArgs.jmhResultJsonFile);
        final List<JmhSummary> jmhSummaries = JmhSummary.ofJmhResult(cliArgs.jmhResultJsonFile);
        dumpReport(cliArgs.outputAdocFile, jmhSetup, jmhSummaries);
    }

    private static final class CliArgs {

        private final File jmhResultJsonFile;

        private final File outputAdocFile;

        private CliArgs(final File jmhResultJsonFile, final File outputAdocFile) {
            this.jmhResultJsonFile = jmhResultJsonFile;
            this.outputAdocFile = outputAdocFile;
        }

        private static CliArgs parseArgs(final String[] args) {

            // Check number of arguments.
            if (args.length != 2) {
                throw new IllegalArgumentException(
                        "usage: <jmhResultJsonFile> <outputAdocFile>");
            }

            // Parse the JMH result JSON file.
            final File jmhResultJsonFile = new File(args[0]);
            if (!jmhResultJsonFile.isFile()) {
                throw new IllegalArgumentException(
                        "jmhResultJsonFile doesn't point to a regular file: " +
                                jmhResultJsonFile);
            }
            if (!jmhResultJsonFile.canRead()) {
                throw new IllegalArgumentException(
                        "jmhResultJsonFile is not readable: " +
                                jmhResultJsonFile);
            }

            // Parse the output AsciiDoc file.
            final File outputAdocFile = new File(args[1]);
            touch(outputAdocFile);

            // Looks okay.
            return new CliArgs(jmhResultJsonFile, outputAdocFile);

        }

        public static void touch(final File file) {
            Objects.requireNonNull(file, "file");
            final Path path = file.toPath();
            try {
                if (Files.exists(path)) {
                    Files.setLastModifiedTime(path, FileTime.from(Instant.now()));
                } else {
                    Files.createFile(path);
                }
            } catch (IOException error) {
                throw new RuntimeException("failed to touch file: " + file, error);
            }
        }

    }

    private static final class JmhSetup {

        private final String vmName;

        private final String vmVersion;

        private final List<String> vmArgs;

        private final int forkCount;

        private final int warmupIterationCount;

        private final String warmupTime;

        private final int measurementIterationCount;

        private final String measurementTime;

        private JmhSetup(
                final String vmName,
                final String vmVersion,
                final List<String> vmArgs,
                final int forkCount,
                final int warmupIterationCount,
                final String warmupTime,
                final int measurementIterationCount,
                final String measurementTime) {
            this.vmName = vmName;
            this.vmVersion = vmVersion;
            this.vmArgs = vmArgs;
            this.forkCount = forkCount;
            this.warmupIterationCount = warmupIterationCount;
            this.warmupTime = warmupTime;
            this.measurementIterationCount = measurementIterationCount;
            this.measurementTime = measurementTime;
        }

        private static JmhSetup ofJmhResult(final File jmhResultFile) throws IOException {
            final List<Object> jmhResult = readObject(jmhResultFile);
            return ofJmhResult(jmhResult);
        }

        private static JmhSetup ofJmhResult(final List<Object> jmhResult) {
            final Object jmhResultEntry = jmhResult.stream().findFirst().get();
            final String vmName = readObjectAtPath(jmhResultEntry, "vmName");
            final String vmVersion = readObjectAtPath(jmhResultEntry, "vmVersion");
            final List<String> vmArgs = readObjectAtPath(jmhResultEntry, "jvmArgs");
            final int forkCount = readObjectAtPath(jmhResultEntry, "forks");
            final int warmupIterationCount = readObjectAtPath(jmhResultEntry, "warmupIterations");
            final String warmupTime = readObjectAtPath(jmhResultEntry, "warmupTime");
            final int measurementIterationCount = readObjectAtPath(jmhResultEntry, "measurementIterations");
            final String measurementTime = readObjectAtPath(jmhResultEntry, "measurementTime");
            return new JmhSetup(
                    vmName,
                    vmVersion,
                    vmArgs,
                    forkCount,
                    warmupIterationCount,
                    warmupTime,
                    measurementIterationCount,
                    measurementTime);
        }

    }

    private static final class JmhSummary {

        private final String benchmark;

        private final BigDecimal opRate;

        private final BigDecimal gcRate;

        private JmhSummary(
                final String benchmark,
                final BigDecimal opRate,
                final BigDecimal gcRate) {
            this.benchmark = benchmark;
            this.opRate = opRate;
            this.gcRate = gcRate;
        }

        private static List<JmhSummary> ofJmhResult(final File jmhResultFile) throws IOException {
            final List<Object> jmhResult = readObject(jmhResultFile);
            return ofJmhResult(jmhResult);
        }

        private static List<JmhSummary> ofJmhResult(final List<Object> jmhResult) {
            final BigDecimal maxOpRate = jmhResult
                    .stream()
                    .map(jmhResultEntry -> readBigDecimalAtPath(jmhResultEntry, "primaryMetric", "scorePercentiles", "99.0"))
                    .max(BigDecimal::compareTo)
                    .get();
            return jmhResult
                    .stream()
                    .map(jmhResultEntry -> {
                        final String benchmark = readObjectAtPath(jmhResultEntry, "benchmark");
                        final BigDecimal opRate = readBigDecimalAtPath(jmhResultEntry, "primaryMetric", "scorePercentiles", "99.0");
                        final BigDecimal gcRate = readBigDecimalAtPath(jmhResultEntry, "secondaryMetrics", "·gc.alloc.rate.norm", "scorePercentiles", "99.0");
                        return new JmhSummary(benchmark, opRate, gcRate);
                    })
                    .collect(Collectors.toList());
        }

    }

    private static <V> V readObject(final File file) throws IOException {
        final byte[] jsonBytes = Files.readAllBytes(file.toPath());
        final String json = new String(jsonBytes, CHARSET);
        @SuppressWarnings("unchecked")
        final V object = (V) JsonReader.read(json);
        return object;
    }

    private static <V> V readObjectAtPath(final Object object, String... path) {
        Object lastObject = object;
        for (final String key : path) {
            @SuppressWarnings("unchecked")
            Map<String, Object> lastMap = (Map<String, Object>) lastObject;
            lastObject = lastMap.get(key);
        }
        @SuppressWarnings("unchecked")
        final V typedLastObject = (V) lastObject;
        return typedLastObject;
    }

    private static BigDecimal readBigDecimalAtPath(final Object object, String... path) {
        final Number number = readObjectAtPath(object, path);
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        } else if (number instanceof Integer) {
            final int intNumber = (int) number;
            return BigDecimal.valueOf(intNumber);
        } else if (number instanceof Long) {
            final long longNumber = (long) number;
            return BigDecimal.valueOf(longNumber);
        } else if (number instanceof BigInteger) {
            final BigInteger bigInteger = (BigInteger) number;
            return new BigDecimal(bigInteger);
        } else {
            final String message = String.format(
                    "failed to convert the value to BigDecimal at path %s: %s",
                    Arrays.asList(path), number);
            throw new IllegalArgumentException(message);
        }
    }

    private static void dumpReport(
            final File outputAdocFile,
            final JmhSetup jmhSetup,
            final List<JmhSummary> jmhSummaries) throws IOException {
        try (final OutputStream outputStream = new FileOutputStream(outputAdocFile);
             final PrintStream printStream = new PrintStream(outputStream, false, CHARSET.name())) {
            dumpJmhSetup(printStream, jmhSetup);
            dumpJmhSummaries(printStream, jmhSummaries, "lite");
            dumpJmhSummaries(printStream, jmhSummaries, "full");
        }
    }

    private static void dumpJmhSetup(
            final PrintStream printStream,
            final JmhSetup jmhSetup) {
        printStream.println("[cols=\"1,4\", options=\"header\"]");
        printStream.println(".JMH setup");
        printStream.println("|===");
        printStream.println("|Setting|Value");
        printStream.format("|JVM name|%s%n", jmhSetup.vmName);
        printStream.format("|JVM version|%s%n", jmhSetup.vmVersion);
        printStream.format("|JVM args|%s%n", jmhSetup.vmArgs != null ? String.join(" ", jmhSetup.vmArgs) : "");
        printStream.format("|Forks|%s%n", jmhSetup.forkCount);
        printStream.format("|Warmup iterations|%d × %s%n", jmhSetup.warmupIterationCount, jmhSetup.warmupTime);
        printStream.format("|Measurement iterations|%d × %s%n", jmhSetup.measurementIterationCount, jmhSetup.measurementTime);
        printStream.println("|===");
    }

    private static void dumpJmhSummaries(
            final PrintStream printStream,
            final List<JmhSummary> jmhSummaries,
            final String prefix) {

        // Print header.
        printStream.println("[cols=\"4,>2,4,>2\", options=\"header\"]");
        printStream.format(".JMH result (99^th^ percentile) summary for \"%s\" log events%n", prefix);
        printStream.println("|===");
        printStream.println("^|Benchmark");
        printStream.println("2+^|ops/sec");
        printStream.println("^|B/op");

        // Filter JMH summaries by prefix.
        final String filterRegex = String.format("^.*\\.%s[A-Za-z0-9]+$", prefix);
        final List<JmhSummary> filteredJmhSummaries = jmhSummaries
                .stream()
                .filter(jmhSummary -> jmhSummary.benchmark.matches(filterRegex))
                .collect(Collectors.toList());

        // Determine the max. op rate.
        final BigDecimal maxOpRate = filteredJmhSummaries
                .stream()
                .map(jmhSummary -> jmhSummary.opRate)
                .max(BigDecimal::compareTo)
                .get();

        // Print each summary.
        final Comparator<JmhSummary> jmhSummaryComparator =
                Comparator
                        .comparing((final JmhSummary jmhSummary) -> jmhSummary.opRate)
                        .reversed();
        filteredJmhSummaries
                .stream()
                .sorted(jmhSummaryComparator)
                .forEach((final JmhSummary jmhSummary) -> {
                    dumpJmhSummary(printStream, maxOpRate, jmhSummary);
                });

        // Print footer.
        printStream.println("|===");

    }

    private static void dumpJmhSummary(
            final PrintStream printStream,
            final BigDecimal maxOpRate,
            final JmhSummary jmhSummary) {
        printStream.println();
        final String benchmark = jmhSummary
                .benchmark
                .replaceAll("^.*\\.([^.]+)$", "$1");
        printStream.format("|%s%n", benchmark);
        final long opRatePerSec = jmhSummary
                .opRate
                .multiply(BigDecimal.valueOf(1_000L))
                .toBigInteger()
                .longValueExact();
        printStream.format("|%,d%n", opRatePerSec);
        final BigDecimal normalizedOpRate = jmhSummary
                .opRate
                .divide(maxOpRate, RoundingMode.CEILING);
        final int opRateBarLength = normalizedOpRate
                .multiply(BigDecimal.valueOf(19))
                .toBigInteger()
                .add(BigInteger.ONE)
                .intValueExact();
        final String opRateBar = Strings.repeat("▉", opRateBarLength);
        final int opRatePercent = normalizedOpRate
                .multiply(BigDecimal.valueOf(100))
                .toBigInteger()
                .intValueExact();
        printStream.format("|%s (%d%%)%n", opRateBar, opRatePercent);
        printStream.format("|%,.1f%n", jmhSummary.gcRate.doubleValue());
    }

}
