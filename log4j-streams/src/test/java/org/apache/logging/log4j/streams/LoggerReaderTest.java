/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.streams;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.CharBuffer;

import org.junit.Before;
import org.junit.Test;

public class LoggerReaderTest extends AbstractStreamTest {
    protected StringReader wrapped;
    protected StringWriter read;
    protected Reader reader;

    @Before
    public void createStream() {
        wrapped = new StringReader(FIRST + "\r\n" + LAST);
        read = new StringWriter();
        reader = createReader();
    }
    
    protected Reader createReader() {
        return new LoggerReader(wrapped, getLogger(), LEVEL);
    }

    @Test
    public void testRead_int() throws Exception {
        for (int i = 0; i < FIRST.length(); i++) {
            read.write(reader.read());
        }
        if (!(reader instanceof BufferedReader)) {
            assertMessages();
        }
        assertEquals("carriage return", '\r', reader.read());
        if (!(reader instanceof BufferedReader)) {
            assertMessages();
        }
        assertEquals("newline", '\n', reader.read());
        assertMessages(FIRST);
    }

    @Test
    public void testRead_CharArray() throws Exception {
        final char[] chars = new char[FIRST.length()];
        assertEquals("len", FIRST.length(), reader.read(chars));
        if (!(reader instanceof BufferedReader)) {
            assertMessages();
        }
        reader.read(chars);
        assertMessages(FIRST);
    }

    @Test
    public void testRead_CharArray_Offset_Length() throws Exception {
        final char[] chars = new char[1024];
        assertEquals("len", FIRST.length(), reader.read(chars, 0, FIRST.length()));
        if (!(reader instanceof BufferedReader)) {
            assertMessages();
        }
        reader.read(chars);
        reader.close();
        assertMessages(FIRST, LAST);
    }

    @Test
    public void testRead_CharBuffer() throws Exception {
        final CharBuffer chars = CharBuffer.allocate(1024);
        assertEquals("len", FIRST.length() + LAST.length() + 2, reader.read(chars));
        reader.close();
        assertMessages(FIRST, LAST);
    }

    @Test
    public void testRead_IgnoresWindowsNewline() throws IOException {
        final char[] chars = new char[1024];
        final int len = reader.read(chars);
        read.write(chars, 0, len);
        if (!(reader instanceof BufferedReader)) {
            assertMessages(FIRST);
        }
        assertEquals(FIRST + "\r\n" + LAST, read.toString());
        reader.close();
        assertMessages(FIRST, LAST);
    }

    @Test
    public void testRead_MultipleLines() throws IOException {
        wrapped = new StringReader(FIRST + "\n" + LAST + '\n');
        reader = createReader();

        final char[] chars = new char[1024];
        final int len = reader.read(chars);
        read.write(chars, 0, len);
        assertMessages(FIRST, LAST);
        assertEquals(FIRST + '\n' + LAST + '\n', read.toString());
    }

    @Test
    public void testClose_NoRemainingData() throws IOException {
        wrapped = new StringReader(FIRST + '\n');
        reader = createReader();

        final char[] chars = new char[1024];
        reader.read(chars);
        assertMessages(FIRST);
        reader.close();
        assertMessages(FIRST);
    }

    @Test
    public void testClose_HasRemainingData() throws IOException {
        final char[] chars = new char[1024];
        reader.read(chars);
        if (!(reader instanceof BufferedReader)) {
            assertMessages(FIRST);
        }
        reader.close();
        assertMessages(FIRST, LAST);
    }
}
