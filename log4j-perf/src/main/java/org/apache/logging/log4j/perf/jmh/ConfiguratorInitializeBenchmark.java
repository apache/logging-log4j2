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

package org.apache.logging.log4j.perf.jmh;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * This benchmark demonstrates how long it takes for a simple XML configuration file to be parsed and initialize a new
 * {@link org.apache.logging.log4j.core.LoggerContext} using that configuration.
 */
// TO RUN THIS TEST:
// java -jar target/benchmarks.jar '.*ConfiguratorInitializeBenchmark.*'
@State(Scope.Thread)
public class ConfiguratorInitializeBenchmark {

    private static final String inlineConfigurationXML = "<Configuration name='ConfiguratorInitializeTest' status='off'>"
            + "<Appenders>"
            + "<Console name='STDOUT'>"
            + "<PatternLayout pattern='%m%n'/>"
            + "</Console>"
            + "</Appenders>"
            + "<Loggers>"
            + "<Root level='error'>"
            + "<AppenderRef ref='STDOUT'/>"
            + "</Root>"
            + "</Loggers>" + "</Configuration>";

    private ConfigurationSource configurationSource;

    @Setup
    public void setUp() throws IOException {
        configurationSource = new ConfigurationSource(new ByteArrayInputStream(inlineConfigurationXML.getBytes()));
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public LoggerContext initializeLoggerContext() {
        return Configurator.initialize(null, configurationSource);
    }

}
