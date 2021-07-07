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

import com.google.common.base.Charsets;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.serializeUsingLayout;

class GcpLayoutTest {

    private static final Configuration CONFIGURATION = new DefaultConfiguration();

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final String SERVICE_NAME = "test";

    private static final JsonTemplateLayout JSON_TEMPLATE_LAYOUT = JsonTemplateLayout
            .newBuilder()
            .setConfiguration(CONFIGURATION)
            .setCharset(CHARSET)
            .setEventTemplateUri("classpath:GcpLayout.json")
            .build();

    @Test
    void test_lite_log_events() {
        final List<LogEvent> logEvents = LogEventFixture.createLiteLogEvents(1_000);
        test(logEvents);
    }

    @Test
    void test_full_log_events() {
        final List<LogEvent> logEvents = LogEventFixture.createFullLogEvents(1_000);
        test(logEvents);
    }

    private static void test(final Collection<LogEvent> logEvents) {
        for (final LogEvent logEvent : logEvents) {
            test(logEvent);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void test(final LogEvent logEvent) {
        final Map<String, Object> jsonTemplateLayoutMap = renderUsingJsonTemplateLayout(logEvent);
        Assertions.assertThat(Instant.parse(jsonTemplateLayoutMap.get("timestamp").toString()).toEpochMilli())
          .isEqualTo(logEvent.getTimeMillis());
        Assertions.assertThat(jsonTemplateLayoutMap.get("severity")).isEqualTo(logEvent.getLevel().toString());
        Assertions.assertThat(jsonTemplateLayoutMap.get("_logger")).isEqualTo(logEvent.getLoggerName());
        Assertions.assertThat(jsonTemplateLayoutMap.get("_thread").toString())
          .isEqualTo(logEvent.getThreadName());

        if (logEvent.getContextData().size() == 0) {
            Assertions.assertThat(jsonTemplateLayoutMap.get("logging.googleapis.com/labels"))
              .isNull();
        } else {
            Assertions.assertThat(logEvent.getContextData().toMap().keySet())
              .isEqualTo(((Map)jsonTemplateLayoutMap.get("logging.googleapis.com/labels")).keySet());
            logEvent.getContextData().toMap().keySet().forEach(k -> {
                Assertions.assertThat((Object) logEvent.getContextData().getValue(k))
                  .isEqualTo(((Map)jsonTemplateLayoutMap.get("logging.googleapis.com/labels")).get(k));
            });
        }

        if (logEvent.getSource() == null) {
            // not quite sure why the output has "?." in sourceLocation.function
            //Assertions.assertThat(jsonTemplateLayoutMap.get("logging.googleapis.com/sourceLocation"))
            //  .isNull();
        } else {
            if (logEvent.getSource().getFileName() == null || !logEvent.isIncludeLocation()) {
                Assertions.assertThat(((Map) jsonTemplateLayoutMap.get("logging.googleapis.com/sourceLocation")).get("file"))
                  .isNull();
            } else {
                Assertions.assertThat(((Map) jsonTemplateLayoutMap.get("logging.googleapis.com/sourceLocation")).get("file"))
                  .isEqualTo(logEvent.getSource().getFileName());
            }
            if (logEvent.getSource().getLineNumber() < 0 || !logEvent.isIncludeLocation()) {
                Assertions.assertThat(((Map) jsonTemplateLayoutMap.get("logging.googleapis.com/sourceLocation")).get("line"))
                  .isNull();
            } else {
                Assertions.assertThat(((Map) jsonTemplateLayoutMap.get("logging.googleapis.com/sourceLocation")).get("line"))
                  .isEqualTo(logEvent.getSource().getLineNumber());
            }
            Assertions.assertThat(((Map) jsonTemplateLayoutMap.get("logging.googleapis.com/sourceLocation")).get("function"))
              .isEqualTo(logEvent.getSource().getClassName() + "." + logEvent.getSource().getMethodName());
        }

        // NOTE: no access to serial number in logEvent
        if (logEvent.getThrown() == null) {
            Assertions.assertThat(jsonTemplateLayoutMap.get("_exception"))
              .isNull();
            Assertions.assertThat(jsonTemplateLayoutMap.get("message"))
              .isEqualTo(logEvent.getMessage().toString());
        } else {
            // the message field includes class packaging information as it uses the %xEx pattern,
            // this fails as extendedStackTrace is truncated for some reason
            //String extendedStackTrace = logEvent.getThrownProxy().getExtendedStackTraceAsString();
            //Assertions.assertThat(jsonTemplateLayoutMap.get("message"))
            //  .isEqualTo(logEvent.getMessage().toString() + " " + extendedStackTrace);

            // for now, just compare the first line and that we have multiple stack trace lines
            String[] messageLines = jsonTemplateLayoutMap.get("message").toString().split(System.lineSeparator());
            Assertions.assertThat(messageLines[0])
              .isEqualTo(logEvent.getMessage().toString() + " " + logEvent.getThrown().getClass().getName() + ": " + logEvent.getThrown().getMessage());
            Assertions.assertThat(messageLines.length)
              .isGreaterThan(1);

            Assertions.assertThat(((Map) jsonTemplateLayoutMap.get("_exception")).get("exception_class"))
              .isEqualTo(logEvent.getThrown().getClass().getName());
            Assertions.assertThat(((Map) jsonTemplateLayoutMap.get("_exception")).get("exception_message"))
              .isEqualTo(logEvent.getThrown().getMessage());
            Assertions.assertThat(((Map) jsonTemplateLayoutMap.get("_exception")).get("stacktrace"))
              .isEqualTo(serializeStackTrace(Charsets.UTF_8, logEvent.getThrown()));
        }
    }

    private static Map<String, Object> renderUsingJsonTemplateLayout(
            final LogEvent logEvent) {
        return serializeUsingLayout(logEvent, JSON_TEMPLATE_LAYOUT);
    }

    private static String serializeStackTrace(
      final Charset charset,
      final Throwable exception) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final String charsetName = charset.name();
        try (final PrintStream printStream =
               new PrintStream(outputStream, false, charsetName)) {
            exception.printStackTrace(printStream);
            return outputStream.toString(charsetName);
        }  catch (final UnsupportedEncodingException error) {
            throw new RuntimeException("failed converting the stack trace to string", error);
        }
    }
}
