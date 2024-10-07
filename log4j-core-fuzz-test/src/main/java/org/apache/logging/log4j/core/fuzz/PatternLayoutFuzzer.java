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
package org.apache.logging.log4j.core.fuzz;

import static org.apache.logging.log4j.fuzz.FuzzingUtil.createLoggerContext;
import static org.apache.logging.log4j.fuzz.FuzzingUtil.fuzzLogger;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.fuzz.EncodingAppender;
import org.apache.logging.log4j.fuzz.FuzzingUtil.Log4jLoggerFacade;
import org.apache.logging.log4j.fuzz.FuzzingUtil.LoggerFacade;
import org.apache.logging.log4j.spi.ExtendedLogger;

public final class PatternLayoutFuzzer {

    public static void fuzzerTestOneInput(final FuzzedDataProvider dataProvider) {
        final String loggerContextName = PatternLayoutFuzzer.class.getSimpleName() + "LoggerContext";
        try (final LoggerContext loggerContext =
                createLoggerContext(loggerContextName, EncodingAppender.PLUGIN_NAME, configBuilder -> configBuilder
                        .newLayout("PatternLayout")
                        // Enforce using a single message-based converter, i.e., `MessagePatternConverter`
                        .addAttribute("pattern", "%m"))) {
            final ExtendedLogger logger = loggerContext.getLogger(PatternLayoutFuzzer.class);
            final LoggerFacade loggerFacade = new Log4jLoggerFacade(logger);
            fuzzLogger(loggerFacade, dataProvider);
        }
    }
}
