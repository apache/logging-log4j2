package org.apache.logging.log4j.layout.json.template;

import org.apache.logging.log4j.layout.json.template.util.JsonReader;
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
 *     org.apache.logging.log4j.layout.json.template.JsonTemplateLayoutBenchmarkReport \
 *     log4j-perf/target/JsonTemplateLayoutBenchmarkResult.json \
 *     log4j-perf/target/JsonTemplateLayoutBenchmarkReport.adoc
 * </pre>
 * @see JsonTemplateLayoutBenchmark on how to generate JMH result JSON file
 */
public enum JsonTemplateLayoutBenchmarkReport {;

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    public static void main(final String[] args) throws Exception {
        final CliArgs cliArgs = CliArgs.parseArgs(args);
        final List<JmhSummary> jmhSummaries = JmhSummary.ofJmhResult(cliArgs.jmhResultJsonFile);
        dumpJmhSummaries(cliArgs.outputAdocFile, jmhSummaries);
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

    private static final class JmhSummary {

        private final String benchmark;

        private final BigDecimal opRate;

        private final BigDecimal normalizedOpRate;

        private final BigDecimal gcRate;

        private JmhSummary(
                final String benchmark,
                final BigDecimal opRate,
                final BigDecimal normalizedOpRate,
                final BigDecimal gcRate) {
            this.benchmark = benchmark;
            this.opRate = opRate;
            this.normalizedOpRate = normalizedOpRate;
            this.gcRate = gcRate;
        }

        private static List<JmhSummary> ofJmhResult(final File jmhResultFile) throws IOException {
            final byte[] jmhResultJsonBytes = Files.readAllBytes(jmhResultFile.toPath());
            final String jmhResultJson = new String(jmhResultJsonBytes, CHARSET);
            @SuppressWarnings("unchecked")
            final List<Object> jmhResult = (List<Object>) JsonReader.read(jmhResultJson);
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
                        final BigDecimal normalizedOpRate = opRate.divide(maxOpRate, RoundingMode.CEILING);
                        return new JmhSummary(benchmark, opRate, normalizedOpRate, gcRate);
                    })
                    .collect(Collectors.toList());
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

    }

    private static void dumpJmhSummaries(
            final File outputAdocFile,
            final List<JmhSummary> jmhSummaries) throws IOException {
        try (final OutputStream outputStream = new FileOutputStream(outputAdocFile);
             final PrintStream printStream = new PrintStream(outputStream, false, CHARSET.name())) {
            dumpJmhSummaries(printStream, jmhSummaries);
        }
    }

    private static void dumpJmhSummaries(
            final PrintStream printStream,
            final List<JmhSummary> jmhSummaries) {

        // Print header.
        printStream.println("[cols=\"4,>2,4,>2\", options=\"header\"]");
        printStream.println(".JMH result (99^th^ percentile) summary");
        printStream.println("|===");
        printStream.println("^|Benchmark");
        printStream.println("2+^|ops/sec");
        printStream.println("^|B/op");

        // Print each summary.
        final Comparator<JmhSummary> jmhSummaryComparator =
                Comparator
                        .comparing((final JmhSummary jmhSummary) -> jmhSummary.opRate)
                        .reversed();
        jmhSummaries
                .stream()
                .sorted(jmhSummaryComparator)
                .forEach((final JmhSummary jmhSummary) -> {
                    dumpJmhSummary(printStream, jmhSummary);
                });

        // Print footer.
        printStream.println("|===");

    }

    private static void dumpJmhSummary(PrintStream printStream, JmhSummary jmhSummary) {
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
        final int opRateBarLength = jmhSummary
                .normalizedOpRate
                .multiply(BigDecimal.valueOf(19))
                .toBigInteger()
                .add(BigInteger.ONE)
                .intValueExact();
        final String opRateBar = Strings.repeat("▉", opRateBarLength);
        final int opRatePercent = jmhSummary
                .normalizedOpRate
                .multiply(BigDecimal.valueOf(100))
                .toBigInteger()
                .intValueExact();
        printStream.format("|%s (%d%%)%n", opRateBar, opRatePercent);
        printStream.format("|%,.1f%n", jmhSummary.gcRate.doubleValue());
    }

}
