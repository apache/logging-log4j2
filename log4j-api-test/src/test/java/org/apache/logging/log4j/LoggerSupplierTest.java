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
package org.apache.logging.log4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.JsonMessage;
import org.apache.logging.log4j.message.LocalizedMessage;
import org.apache.logging.log4j.message.MessageFormatMessage;
import org.apache.logging.log4j.message.ObjectArrayMessage;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.message.ThreadDumpMessage;
import org.apache.logging.log4j.test.TestLogger;
import org.apache.logging.log4j.util.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

/**
 * Tests Logger APIs with {@link Supplier}.
 */
@ResourceLock(Resources.LOCALE)
@ResourceLock("log4j2.TestLogger")
class LoggerSupplierTest {

    private final TestLogger logger = (TestLogger) LogManager.getLogger("LoggerTest");

    private final List<String> results = logger.getEntries();

    Locale defaultLocale;

    @Test
    void flowTracing_SupplierOfFormattedMessage() {
        logger.traceEntry(() -> new FormattedMessage("int foo={}", 1234567890));
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("(int foo=1234567890)")
                .doesNotContain("FormattedMessage");
    }

    @Test
    void flowTracing_SupplierOfJsonMessage() {
        final Properties props = new Properties();
        props.setProperty("foo", "bar");
        logger.traceEntry(() -> new JsonMessage(props));
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("\"foo\":\"bar\"")
                .doesNotContain("JsonMessage");
    }

    @Test
    void flowTracing_SupplierOfLocalizedMessage() {
        logger.traceEntry(() -> new LocalizedMessage("int foo={}", 1234567890));
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("(int foo=1234567890)")
                .doesNotContain("LocalizedMessage");
    }

    @Test
    void flowTracing_SupplierOfLong() {
        logger.traceEntry(() -> 1234567890L);
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("(1234567890)")
                .doesNotContain("SimpleMessage");
    }

    @Test
    void flowTracing_SupplierOfMessageFormatMessage() {
        logger.traceEntry(() -> new MessageFormatMessage("int foo={0}", 1234567890));
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("(int foo=1,234,567,890)")
                .doesNotContain("MessageFormatMessage");
    }

    @Test
    void flowTracing_SupplierOfObjectArrayMessage() {
        logger.traceEntry(() -> new ObjectArrayMessage(1234567890));
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("([1234567890])")
                .doesNotContain("ObjectArrayMessage");
    }

    @Test
    void flowTracing_SupplierOfObjectMessage() {
        logger.traceEntry(() -> new ObjectMessage(1234567890));
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("(1234567890)")
                .doesNotContain("ObjectMessage");
    }

    @Test
    void flowTracing_SupplierOfParameterizedMessage() {
        logger.traceEntry(() -> new ParameterizedMessage("int foo={}", 1234567890));
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("(int foo=1234567890)")
                .doesNotContain("ParameterizedMessage");
    }

    @Test
    void flowTracing_SupplierOfSimpleMessage() {
        logger.traceEntry(() -> new SimpleMessage("1234567890"));
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("(1234567890)")
                .doesNotContain("SimpleMessage");
    }

    @Test
    void flowTracing_SupplierOfString() {
        logger.traceEntry(() -> "1234567890");
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("(1234567890)")
                .doesNotContain("SimpleMessage");
    }

    @Test
    void flowTracing_SupplierOfStringFormattedMessage() {
        logger.traceEntry(() -> new StringFormattedMessage("int foo=%,d", 1234567890));
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("(int foo=1,234,567,890)")
                .doesNotContain("StringFormattedMessage");
    }

    @Test
    void flowTracing_SupplierOfThreadDumpMessage() {
        logger.traceEntry(() -> new ThreadDumpMessage("Title of ..."));
        assertThat(results).hasSize(1);
        final String entry = results.get(0);
        assertThat(entry)
                .startsWith("ENTER[ FLOW ] TRACE Enter")
                .contains("RUNNABLE", "Title of ...", getClass().getName());
    }

    @BeforeEach
    void setup() {
        results.clear();
        defaultLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, java.util.Locale.US);
    }

    @AfterEach
    void tearDown() {
        Locale.setDefault(Locale.Category.FORMAT, defaultLocale);
    }
}
