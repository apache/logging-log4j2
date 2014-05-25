/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.core.config.plugins.util;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.util.Charsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 * Unit tests for the various supported TypeConverter implementations.
 */
@RunWith(Parameterized.class)
public class TypeConvertersTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
            //   value, expected, default, type
            new Object[][]{
                // booleans
                { "true", true, null, Boolean.class },
                { "false", false, null, Boolean.class },
                { "True", true, null, Boolean.class },
                { "TRUE", true, null, Boolean.class },
                { "blah", false, null, Boolean.class }, // TODO: is this acceptable? it's how Boolean.parseBoolean works
                { null, null, null, Boolean.class },
                { null, true, "true", Boolean.class },
                { "no", false, null, Boolean.class }, // TODO: see above
                { "true", true, "false", boolean.class },
                { "FALSE", false, "true", boolean.class },
                { null, false, "false", boolean.class },
                { "invalid", false, "false", boolean.class },
                // integers
                { "+42", 42, null, Integer.class },
                { "53", 53, null, Integer.class },
                { "-16", -16, null, Integer.class },
                { "0", 0, null, Integer.class },
                { "n", null, null, Integer.class },
                { "n", 5, "5", Integer.class },
                { "4.2", null, null, Integer.class },
                { "4.2", 0, "0", Integer.class },
                { null, null, null, Integer.class },
                { "75", 75, "0", int.class },
                { "-30", -30, "0", int.class },
                { "0", 0, "10", int.class },
                { null, 10, "10", int.class },
                // longs
                { "55", 55L, null, Long.class },
                { "1234567890123456789", 1234567890123456789L, null, Long.class },
                { "123123123L", null, null, Long.class },
                { "+123123123123", 123123123123L, null, Long.class },
                { "-987654321", -987654321L, null, Long.class },
                { "-45l", null, null, Long.class },
                { "0", 0L, null, Long.class },
                { "asdf", null, null, Long.class },
                { "3.14", null, null, Long.class },
                { "3.14", 0L, "0", Long.class },
                { "*3", 1000L, "1000", Long.class },
                { null, null, null, Long.class },
                { "3000", 3000L, "0", long.class },
                { "-543210", -543210L, "0", long.class },
                { "22.7", -53L, "-53", long.class },
                // charsets
                { "UTF-8", Charsets.UTF_8, null, Charset.class },
                { "ASCII", Charset.forName("ASCII"), "UTF-8", Charset.class },
                { "Not a real charset", Charsets.UTF_8, "UTF-8", Charset.class },
                { null, Charsets.UTF_8, "UTF-8", Charset.class },
                { null, null, null, Charset.class },
                // levels
                { "ERROR", Level.ERROR, null, Level.class },
                { "WARN", Level.WARN, null, Level.class },
                { "FOO", null, null, Level.class },
                { "FOO", Level.DEBUG, "DEBUG", Level.class },
                { "OFF", Level.OFF, null, Level.class },
                { null, null, null, Level.class },
                { null, Level.INFO, "INFO", Level.class },
                // results
                { "ACCEPT", Filter.Result.ACCEPT, null, Filter.Result.class },
                { "NEUTRAL", Filter.Result.NEUTRAL, null, Filter.Result.class },
                { "DENY", Filter.Result.DENY, null, Filter.Result.class },
                { "NONE", null, null, Filter.Result.class },
                { "NONE", Filter.Result.NEUTRAL, "NEUTRAL", Filter.Result.class },
                { null, null, null, Filter.Result.class },
                { null, Filter.Result.ACCEPT, "ACCEPT", Filter.Result.class },
                // syslog facilities
                { "KERN", Facility.KERN, "USER", Facility.class },
                { "mail", Facility.MAIL, "KERN", Facility.class },
                { "Cron", Facility.CRON, null, Facility.class },
                { "not a real facility", Facility.AUTH, "auth", Facility.class },
                { null, null, null, Facility.class },
            }
        );
    }

    private final String value;
    private final Object expected;
    private final String defaultValue;
    private final Class<?> clazz;

    public TypeConvertersTest(final String value, final Object expected, final String defaultValue, final Class<?> clazz) {
        this.value = value;
        this.expected = expected;
        this.defaultValue = defaultValue;
        this.clazz = clazz;
    }

    @Test
    public void testConvert() throws Exception {
        final Object actual = TypeConverters.convert(value, clazz, defaultValue);
        final String assertionMessage = "\nGiven: " + value + "\nDefault: " + defaultValue;
        assertEquals(assertionMessage, expected, actual);
    }
}
