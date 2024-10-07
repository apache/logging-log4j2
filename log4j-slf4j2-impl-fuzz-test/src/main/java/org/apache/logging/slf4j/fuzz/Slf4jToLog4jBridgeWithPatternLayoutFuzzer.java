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
package org.apache.logging.slf4j.fuzz;

import static org.apache.logging.log4j.fuzz.FuzzingUtil.fuzzLogger;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.fuzz.FuzzingUtil.LoggerFacade;

public final class Slf4jToLog4jBridgeWithPatternLayoutFuzzer {

    static {
        final String configurationFile = Slf4jToLog4jBridgeWithPatternLayoutFuzzer.class.getSimpleName() + ".xml";
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, configurationFile);
    }

    private static final LoggerFacade LOGGER =
            Slf4jLoggerFacade.ofClass(Slf4jToLog4jBridgeWithPatternLayoutFuzzer.class);

    public static void fuzzerTestOneInput(final FuzzedDataProvider dataProvider) {
        fuzzLogger(LOGGER, dataProvider);
    }
}
