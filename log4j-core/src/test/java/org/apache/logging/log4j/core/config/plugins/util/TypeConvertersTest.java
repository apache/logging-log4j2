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

import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
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
            //   value, expected, type
            new Object[][]{
                // booleans
                { "true", true, Boolean.class },
                { "false", false, Boolean.class },
                { "True", true, Boolean.class },
                { "TRUE", true, Boolean.class },
                { "blah", false, Boolean.class },
                { null, false, Boolean.class },
                // integers
                { "+42", 42, Integer.class },
                { "53", 53, Integer.class },
                { "-16", -16, Integer.class },
                { "0", 0, Integer.class },
                { "n", null, Integer.class },
                { "4.2", null, Integer.class },
                { null, null, Integer.class },
                // longs
                { "55", 55L, Long.class },
                { "1234567890123456789", 1234567890123456789L, Long.class },
                { "123123123L", null, Long.class },
                { "+123123123123", 123123123123L, Long.class },
                { "-987654321", -987654321L, Long.class },
                { "-45l", null, Long.class },
                { "0", 0L, Long.class },
                { "asdf", null, Long.class },
                { "3.14", null, Long.class },
                { null, null, Long.class },
                // levels
                { "ERROR", Level.ERROR, Level.class },
                { "WARN", Level.WARN, Level.class },
                { "FOO", null, Level.class },
                { "OFF", Level.OFF, Level.class },
                { null, null, Level.class },
                // results
                { "ACCEPT", Filter.Result.ACCEPT, Filter.Result.class },
                { "NEUTRAL", Filter.Result.NEUTRAL, Filter.Result.class },
                { "DENY", Filter.Result.DENY, Filter.Result.class },
                { "NONE", null, Filter.Result.class },
                { null, null, Filter.Result.class }
            }
        );
    }

    private final String value;
    private final Object expected;
    private final Class<?> clazz;

    public TypeConvertersTest(String value, Object expected, Class<?> clazz) {
        this.value = value;
        this.expected = expected;
        this.clazz = clazz;
    }

    @Test
    public void testConvert() throws Exception {
        final Object actual = TypeConverters.convert(value, clazz);
        assertEquals(expected, actual);
    }
}
