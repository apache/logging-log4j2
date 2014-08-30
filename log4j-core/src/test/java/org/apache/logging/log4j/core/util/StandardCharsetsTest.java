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

package org.apache.logging.log4j.core.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class StandardCharsetsTest {

    private final String charsetName;
    private final Charset expectedCharset;

    public StandardCharsetsTest(final String charsetName, final Charset expectedCharset) {
        this.charsetName = charsetName;
        this.expectedCharset = expectedCharset;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[][]{
                { "US-ASCII", Charsets.US_ASCII },
                { "ISO-8859-1", Charsets.ISO_8859_1 },
                { "UTF-8", Charsets.UTF_8 },
                { "UTF-16BE", Charsets.UTF_16BE },
                { "UTF-16LE", Charsets.UTF_16LE },
                { "UTF-16", Charsets.UTF_16 }
            }
        );
    }

    @Test
    public void testSupportsStandardCharset() throws Exception {
        assertTrue(Charset.isSupported(charsetName));
        assertSame(expectedCharset, Charsets.getSupportedCharset(charsetName));
    }

    @Test
    public void testCharsetTranslatesProperly() throws Exception {
        final String expected = "This string contains only ASCII characters to test all the standard charsets";
        final ByteBuffer encoded = expectedCharset.encode(expected);
        final CharBuffer decoded = expectedCharset.decode(encoded);
        final String actual = decoded.toString();
        assertEquals(expected, actual);
    }
}