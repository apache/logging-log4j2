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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.PropertiesLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.trustgate.DefaultInputSanitizer;
import org.apache.logging.log4j.trustgate.TrustGateException;
import org.apache.logging.log4j.trustgate.rules.JndiSchemeValidationRule;
import org.apache.logging.log4j.trustgate.rules.LookupPatternValidationRule;
import org.apache.logging.log4j.trustgate.rules.PropertyKeyValidationRule;
import org.apache.logging.log4j.trustgate.rules.RecursiveLookupValidationRule;
import org.apache.logging.log4j.trustgate.rules.UriSchemeValidationRule;
import org.apache.logging.log4j.trustgate.spi.InputSanitizer;
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Measures TrustGate validation rule and sanitizer overhead. Common-path latency target is below 100 ns per
 * invocation; attack-pattern matching is expected to remain below 500 ns per rule invocation.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Thread)
public class TrustGateValidationBenchmark {

    private static final String FIXTURE = "trustgate-benchmark-inputs.txt";

    private JndiSchemeValidationRule jndiRule;
    private UriSchemeValidationRule uriRule;
    private LookupPatternValidationRule lookupRule;
    private RecursiveLookupValidationRule recursiveRule;
    private PropertyKeyValidationRule propertyKeyRule;
    private InputSanitizer sanitizer;
    private StrSubstitutor substitutor;

    private String commonLogMessage;
    private String commonJndiLookup;
    private String commonUriScheme;
    private String commonConfigurationValue;
    private String commonLookupPattern;
    private String commonPropertyKey;
    private String attackJndiLookup;
    private String attackUriScheme;
    private String attackConfigurationValue;
    private String attackLogMessage;
    private String attackLookupPattern;
    private String attackRecursiveLookupPattern;
    private String attackPropertyKey;
    private String uriFileScheme;
    private String strSubstitutorPlain;
    private String strSubstitutorLookup;
    private String strSubstitutorMultiLookup;
    private String longLogMessage;

    @Setup
    public void setup() {
        final Map<String, String> inputs = loadInputs();
        jndiRule = new JndiSchemeValidationRule();
        uriRule = new UriSchemeValidationRule();
        lookupRule = new LookupPatternValidationRule();
        recursiveRule = new RecursiveLookupValidationRule();
        propertyKeyRule = new PropertyKeyValidationRule();
        sanitizer = new DefaultInputSanitizer();

        commonLogMessage = inputs.get("commonLogMessage");
        commonJndiLookup = inputs.get("commonJndiLookup");
        commonUriScheme = inputs.get("commonUriScheme");
        commonConfigurationValue = inputs.get("commonConfigurationValue");
        commonLookupPattern = inputs.get("commonLookupPattern");
        commonPropertyKey = inputs.get("commonPropertyKey");
        attackJndiLookup = inputs.get("attackJndiLookup");
        attackUriScheme = inputs.get("attackUriScheme");
        attackConfigurationValue = inputs.get("attackConfigurationValue");
        attackLogMessage = inputs.get("attackLogMessage");
        attackLookupPattern = inputs.get("attackLookupPattern");
        attackRecursiveLookupPattern = inputs.get("attackRecursiveLookupPattern");
        attackPropertyKey = inputs.get("attackPropertyKey");
        uriFileScheme = inputs.get("uriFileScheme");
        strSubstitutorPlain = inputs.get("strSubstitutorPlain");
        strSubstitutorLookup = inputs.get("strSubstitutorLookup");
        strSubstitutorMultiLookup = inputs.get("strSubstitutorMultiLookup");
        longLogMessage = buildLongLogMessage(inputs);

        final Map<String, String> properties = new HashMap<>();
        properties.put("app.name", "benchmark-app");
        final StrLookup lookup = new Interpolator(new PropertiesLookup(properties));
        substitutor = new StrSubstitutor(lookup);

        // Warm ServiceLoader-backed rules before measurement iterations.
        sanitizer.validate(commonLogMessage, InputType.LOG_MESSAGE);
    }

    // --- Individual rules: common path (non-matching) ---

    @Benchmark
    public void jndiRuleCommonPath(final Blackhole bh) {
        bh.consume(jndiRule.matches(commonJndiLookup, InputType.JNDI_LOOKUP));
    }

    @Benchmark
    public void uriRuleCommonPath(final Blackhole bh) {
        bh.consume(uriRule.matches(commonUriScheme, InputType.URI_SCHEME));
    }

    @Benchmark
    public void uriRuleConfigurationCommonPath(final Blackhole bh) {
        bh.consume(uriRule.matches(commonConfigurationValue, InputType.CONFIGURATION_VALUE));
    }

    @Benchmark
    public void lookupRuleCommonPath(final Blackhole bh) {
        bh.consume(lookupRule.matches(commonLogMessage, InputType.LOG_MESSAGE));
    }

    @Benchmark
    public void recursiveRuleCommonPath(final Blackhole bh) {
        bh.consume(recursiveRule.matches(commonLookupPattern, InputType.LOOKUP_PATTERN));
    }

    @Benchmark
    public void propertyKeyRuleCommonPath(final Blackhole bh) {
        bh.consume(propertyKeyRule.matches(commonPropertyKey, InputType.PROPERTY_KEY));
    }

    // --- Individual rules: attack path (matching) ---

    @Benchmark
    public void jndiRuleAttackPath(final Blackhole bh) {
        bh.consume(jndiRule.matches(attackJndiLookup, InputType.JNDI_LOOKUP));
    }

    @Benchmark
    public void uriRuleAttackPath(final Blackhole bh) {
        bh.consume(uriRule.matches(attackUriScheme, InputType.URI_SCHEME));
    }

    @Benchmark
    public void uriRuleConfigurationAttackPath(final Blackhole bh) {
        bh.consume(uriRule.matches(attackConfigurationValue, InputType.CONFIGURATION_VALUE));
    }

    @Benchmark
    public void lookupRuleAttackPath(final Blackhole bh) {
        bh.consume(lookupRule.matches(attackLogMessage, InputType.LOG_MESSAGE));
    }

    @Benchmark
    public void recursiveRuleAttackPath(final Blackhole bh) {
        bh.consume(recursiveRule.matches(attackRecursiveLookupPattern, InputType.LOOKUP_PATTERN));
    }

    @Benchmark
    public void propertyKeyRuleAttackPath(final Blackhole bh) {
        bh.consume(propertyKeyRule.matches(attackPropertyKey, InputType.PROPERTY_KEY));
    }

    // --- DefaultInputSanitizer end-to-end ---

    @Benchmark
    public void sanitizerCommonLogMessage(final Blackhole bh) {
        bh.consume(sanitizer.validate(commonLogMessage, InputType.LOG_MESSAGE).isValid());
    }

    @Benchmark
    public void sanitizerCommonJndiLookup(final Blackhole bh) {
        bh.consume(sanitizer.validate(commonJndiLookup, InputType.JNDI_LOOKUP).isValid());
    }

    @Benchmark
    public void sanitizerCommonUriScheme(final Blackhole bh) {
        bh.consume(sanitizer.validate(uriFileScheme, InputType.URI_SCHEME).isValid());
    }

    @Benchmark
    public void sanitizerCommonLookupPattern(final Blackhole bh) {
        bh.consume(sanitizer
                .validate(commonLookupPattern, InputType.LOOKUP_PATTERN)
                .isValid());
    }

    @Benchmark
    public void sanitizerAttackJndiLookup(final Blackhole bh) {
        try {
            sanitizer.validate(attackJndiLookup, InputType.JNDI_LOOKUP);
            bh.consume(false);
        } catch (final TrustGateException ex) {
            bh.consume(true);
        }
    }

    @Benchmark
    public void sanitizerAttackLookupPattern(final Blackhole bh) {
        try {
            sanitizer.validate(attackLookupPattern, InputType.LOOKUP_PATTERN);
            bh.consume(false);
        } catch (final TrustGateException ex) {
            bh.consume(true);
        }
    }

    @Benchmark
    public void sanitizerLongLogMessage(final Blackhole bh) {
        bh.consume(sanitizer.validate(longLogMessage, InputType.LOG_MESSAGE).isValid());
    }

    // --- StrSubstitutor integration throughput ---

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void strSubstitutorPlainText(final Blackhole bh) {
        bh.consume(substitutor.replace(strSubstitutorPlain));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void strSubstitutorWithTrustGateLookup(final Blackhole bh) {
        bh.consume(substitutor.replace(strSubstitutorLookup));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Benchmark
    public void strSubstitutorWithTrustGateMultiLookup(final Blackhole bh) {
        bh.consume(substitutor.replace(strSubstitutorMultiLookup));
    }

    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Fork(value = 1, jvmArgs = "-Dlog4j2.trustgate.strictness=disabled")
    @Benchmark
    public void strSubstitutorWithoutTrustGateLookup(final Blackhole bh) {
        bh.consume(substitutor.replace(strSubstitutorLookup));
    }

    private static Map<String, String> loadInputs() {
        final Map<String, String> inputs = new HashMap<>();
        try (InputStream input =
                TrustGateValidationBenchmark.class.getClassLoader().getResourceAsStream(FIXTURE)) {
            if (input == null) {
                throw new IllegalStateException("Missing fixture: " + FIXTURE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    final int separator = line.indexOf('=');
                    if (separator <= 0) {
                        continue;
                    }
                    inputs.put(line.substring(0, separator), line.substring(separator + 1));
                }
            }
        } catch (final IOException ex) {
            throw new IllegalStateException("Unable to read fixture: " + FIXTURE, ex);
        }
        return Collections.unmodifiableMap(inputs);
    }

    private static String buildLongLogMessage(final Map<String, String> inputs) {
        final String prefix = inputs.getOrDefault("longStringPrefix", "INFO ");
        final int repeatCount = Integer.parseInt(inputs.getOrDefault("longStringRepeatCount", "200"));
        final StringBuilder builder = new StringBuilder(prefix.length() + repeatCount * 32);
        builder.append(prefix);
        for (int i = 0; i < repeatCount; i++) {
            builder.append("request-").append(i).append(' ');
        }
        builder.append("completed");
        return builder.toString();
    }
}
