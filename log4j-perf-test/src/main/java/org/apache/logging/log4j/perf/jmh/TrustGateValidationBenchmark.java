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

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.trustgate.rules.JndiSchemeValidationRule;
import org.apache.logging.log4j.trustgate.rules.LookupPatternValidationRule;
import org.apache.logging.log4j.trustgate.rules.PropertyKeyValidationRule;
import org.apache.logging.log4j.trustgate.rules.RecursiveLookupValidationRule;
import org.apache.logging.log4j.trustgate.rules.UriSchemeValidationRule;
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Measures TrustGate validation rule overhead on common safe inputs. Target common-path latency is
 * below 100 ns per rule invocation.
 */
@State(Scope.Thread)
public class TrustGateValidationBenchmark {

    private JndiSchemeValidationRule jndiRule;
    private UriSchemeValidationRule uriRule;
    private LookupPatternValidationRule lookupRule;
    private RecursiveLookupValidationRule recursiveRule;
    private PropertyKeyValidationRule propertyKeyRule;

    @Setup
    public void setup() {
        jndiRule = new JndiSchemeValidationRule();
        uriRule = new UriSchemeValidationRule();
        lookupRule = new LookupPatternValidationRule();
        recursiveRule = new RecursiveLookupValidationRule();
        propertyKeyRule = new PropertyKeyValidationRule();
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Benchmark
    public void jndiSafeLookup(final Blackhole bh) {
        bh.consume(jndiRule.matches("java:comp/env/jdbc/datasource", InputType.JNDI_LOOKUP));
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Benchmark
    public void uriSafeScheme(final Blackhole bh) {
        bh.consume(uriRule.matches("https", InputType.URI_SCHEME));
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Benchmark
    public void configurationSafeValue(final Blackhole bh) {
        bh.consume(uriRule.matches("classpath:log4j2.xml", InputType.CONFIGURATION_VALUE));
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Benchmark
    public void safeLogMessage(final Blackhole bh) {
        bh.consume(lookupRule.matches("user logged in successfully", InputType.LOG_MESSAGE));
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Benchmark
    public void safeLookupPattern(final Blackhole bh) {
        bh.consume(recursiveRule.matches("${env:USER}", InputType.LOOKUP_PATTERN));
    }

    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Benchmark
    public void safePropertyKey(final Blackhole bh) {
        bh.consume(propertyKeyRule.matches("log4j2.trustgate.strict", InputType.PROPERTY_KEY));
    }
}
