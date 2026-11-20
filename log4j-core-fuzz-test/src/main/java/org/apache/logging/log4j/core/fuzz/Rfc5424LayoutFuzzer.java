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
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.fuzz.EncodingAppender;
import org.apache.logging.log4j.fuzz.FuzzingUtil.Log4jLoggerFacade;
import org.apache.logging.log4j.fuzz.FuzzingUtil.LoggerFacade;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Fuzzes {@code Rfc5424Layout} with newline escaping enabled. The MDC key and value are
 * fuzzed separately from the message, since context values are the usual untrusted input.
 */
public final class Rfc5424LayoutFuzzer {

    public static void fuzzerTestOneInput(final FuzzedDataProvider dataProvider) {
        final String loggerContextName = Rfc5424LayoutFuzzer.class.getSimpleName() + "LoggerContext";
        try (final LoggerContext loggerContext =
                createLoggerContext(loggerContextName, EncodingAppender.PLUGIN_NAME, configBuilder -> configBuilder
                        .newLayout("Rfc5424Layout")
                        .setAttribute("appName", "fuzz")
                        .setAttribute("newLineEscape", "\\n"))) {
            final ExtendedLogger logger = loggerContext.getLogger(Rfc5424LayoutFuzzer.class);
            final LoggerFacade loggerFacade = new Log4jLoggerFacade(logger);
            final String mdcKey = dataProvider.consumeString(64);
            final String mdcValue = dataProvider.consumeString(256);
            ThreadContext.put(mdcKey, mdcValue);
            try {
                fuzzLogger(loggerFacade, dataProvider);
            } finally {
                ThreadContext.clearMap();
            }
        }
    }
}
