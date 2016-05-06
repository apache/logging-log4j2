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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Utility class that can read the "Ranking" output of the PerfTestDriver and
 * format it for pasting into Excel.
 */
class PerfTestResultFormatter {
    static final String LF = System.lineSeparator();
    static final NumberFormat NUM = new DecimalFormat("#,##0");

    static class Stats {
        long throughput;
        double avgLatency;
        double latency99Pct;
        double latency99_99Pct;

        Stats(final String throughput, final String avg, final String lat99, final String lat99_99)
                throws ParseException {
            this.throughput = NUM.parse(throughput.trim()).longValue();
            this.avgLatency = Double.parseDouble(avg.trim());
            this.latency99Pct = Double.parseDouble(lat99.trim());
            this.latency99_99Pct = Double.parseDouble(lat99_99.trim());
        }
    }

    private final Map<String, Map<String, Stats>> results = new TreeMap<>();

    public PerfTestResultFormatter() {
    }

    public String format(final String text) throws ParseException {
        results.clear();
        final String[] lines = text.split("[\\r\\n]+");
        for (final String line : lines) {
            process(line);
        }
        return latencyTable() + LF + throughputTable();
    }

    private String latencyTable() {
        final StringBuilder sb = new StringBuilder(4 * 1024);
        final Set<String> subKeys = results.values().iterator().next().keySet();
        final char[] tabs = new char[subKeys.size()];
        Arrays.fill(tabs, '\t');
        final String sep = new String(tabs);
        sb.append("\tAverage latency").append(sep).append("99% less than").append(sep).append("99.99% less than");
        sb.append(LF);
        for (int i = 0; i < 3; i++) {
            for (final String subKey : subKeys) {
                sb.append('\t').append(subKey);
            }
        }
        sb.append(LF);
        for (final String key : results.keySet()) {
            sb.append(key);
            for (int i = 0; i < 3; i++) {
                final Map<String, Stats> sub = results.get(key);
                for (final String subKey : sub.keySet()) {
                    final Stats stats = sub.get(subKey);
                    switch (i) {
                    case 0:
                        sb.append('\t').append((long) stats.avgLatency);
                        break;
                    case 1:
                        sb.append('\t').append((long) stats.latency99Pct);
                        break;
                    case 2:
                        sb.append('\t').append((long) stats.latency99_99Pct);
                        break;
                    }
                }
            }
            sb.append(LF);
        }
        return sb.toString();
    }

    private String throughputTable() {
        final StringBuilder sb = new StringBuilder(4 * 1024);
        final Set<String> subKeys = results.values().iterator().next().keySet();
        sb.append("\tThroughput per thread (msg/sec)");
        sb.append(LF);
        for (final String subKey : subKeys) {
            sb.append('\t').append(subKey);
        }
        sb.append(LF);
        for (final String key : results.keySet()) {
            sb.append(key);
            final Map<String, Stats> sub = results.get(key);
            for (final String subKey : sub.keySet()) {
                final Stats stats = sub.get(subKey);
                sb.append('\t').append(stats.throughput);
            }
            sb.append(LF);
        }
        return sb.toString();
    }

    private void process(final String line) throws ParseException {
        final String key = line.substring(line.indexOf('.') + 1, line.indexOf('('));
        final String sub = line.substring(line.indexOf('(') + 1, line.indexOf(')'));
        final String throughput = line.substring(line.indexOf("throughput: ")
                + "throughput: ".length(), line.indexOf(" ops"));
        final String avg = line.substring(line.indexOf("avg=") + "avg=".length(),
                line.indexOf(" 99%"));
        final String pct99 = line.substring(
                line.indexOf("99% < ") + "99% < ".length(),
                line.indexOf(" 99.99%"));
        final String pct99_99 = line.substring(line.indexOf("99.99% < ")
                + "99.99% < ".length(), line.lastIndexOf('(') - 1);
        final Stats stats = new Stats(throughput, avg, pct99, pct99_99);
        Map<String, Stats> map = results.get(key.trim());
        if (map == null) {
            map = new TreeMap<>(sort());
            results.put(key.trim(), map);
        }
        String subKey = sub.trim();
        if ("single thread".equals(subKey)) {
            subKey = "1 thread";
        }
        map.put(subKey, stats);
    }

    private Comparator<String> sort() {
        return new Comparator<String>() {
            List<String> expected = Arrays.asList("1 thread", "2 threads",
                    "4 threads", "8 threads", "16 threads", "32 threads",
                    "64 threads");

            @Override
            public int compare(final String o1, final String o2) {
                final int i1 = expected.indexOf(o1);
                final int i2 = expected.indexOf(o2);
                if (i1 < 0 || i2 < 0) {
                    return o1.compareTo(o2);
                }
                return i1 - i2;
            }
        };
    }

    public static void main(final String[] args) throws Exception {
        final PerfTestResultFormatter fmt = new PerfTestResultFormatter();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(
                System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            fmt.process(line);
        }
        System.out.println(fmt.latencyTable());
        System.out.println();
        System.out.println(fmt.throughputTable());
    }
}
