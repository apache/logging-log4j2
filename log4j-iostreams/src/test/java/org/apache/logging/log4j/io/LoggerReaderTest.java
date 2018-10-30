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
package org.apache.logging.log4j.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.CharBuffer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoggerReaderTest extends AbstractStreamTest {
    protected StringReader wrapped;
    protected StringWriter read;
    protected Reader reader;

    protected Reader createReader() {
        return IoBuilder.forLogger(getExtendedLogger())
            .filter(this.wrapped)
            .setLevel(LEVEL)
            .buildReader();
    }
    
    @Before
    public void createStream() {
        this.wrapped = new StringReader(FIRST + "\r\n" + LAST);
        this.read = new StringWriter();
        this.reader = createReader();
    }

    @Test
    public void testClose_HasRemainingData() throws IOException {
        final char[] chars = new char[1024];
        this.reader.read(chars);
        if (!(this.reader instanceof BufferedReader)) {
            assertMessages(FIRST);
        }
        this.reader.close();
        assertMessages(FIRST, LAST);
    }

    @Test
    public void testClose_NoRemainingData() throws IOException {
        this.wrapped = new StringReader(FIRST + '\n');
        this.reader = createReader();

        final char[] chars = new char[1024];
        this.reader.read(chars);
        assertMessages(FIRST);
        this.reader.close();
        assertMessages(FIRST);
    }

    @Test
    public void testRead_CharArray() throws Exception {
        final char[] chars = new char[FIRST.length()];
        assertEquals("len", FIRST.length(), this.reader.read(chars));
        if (!(this.reader instanceof BufferedReader)) {
            assertMessages();
        }
        this.reader.read(chars);
        assertMessages(FIRST);
    }

    @Test
    public void testRead_CharArray_Offset_Length() throws Exception {
        final char[] chars = new char[1024];
        assertEquals("len", FIRST.length(), this.reader.read(chars, 0, FIRST.length()));
        if (!(this.reader instanceof BufferedReader)) {
            assertMessages();
        }
        this.reader.read(chars);
        this.reader.close();
        assertMessages(FIRST, LAST);
    }

    @Test
    public void testRead_CharBuffer() throws Exception {
        final CharBuffer chars = CharBuffer.allocate(1024);
        assertEquals("len", FIRST.length() + LAST.length() + 2, this.reader.read(chars));
        this.reader.close();
        assertMessages(FIRST, LAST);
    }

    @Test
    public void testRead_IgnoresWindowsNewline() throws IOException {
        final char[] chars = new char[1024];
        final int len = this.reader.read(chars);
        this.read.write(chars, 0, len);
        if (!(this.reader instanceof BufferedReader)) {
            assertMessages(FIRST);
        }
        assertEquals(FIRST + "\r\n" + LAST, this.read.toString());
        this.reader.close();
        assertMessages(FIRST, LAST);
    }

    @Test
    public void testRead_int() throws Exception {
        for (int i = 0; i < FIRST.length(); i++) {
            this.read.write(this.reader.read());
        }
        if (!(this.reader instanceof BufferedReader)) {
            assertMessages();
        }
        assertEquals("carriage return", '\r', this.reader.read());
        if (!(this.reader instanceof BufferedReader)) {
            assertMessages();
        }
        assertEquals("newline", '\n', this.reader.read());
        assertMessages(FIRST);
    }

    @Test
    public void testRead_MultipleLines() throws IOException {
        this.wrapped = new StringReader(FIRST + "\n" + LAST + '\n');
        this.reader = createReader();

        final char[] chars = new char[1024];
        final int len = this.reader.read(chars);
        this.read.write(chars, 0, len);
        assertMessages(FIRST, LAST);
        assertEquals(FIRST + '\n' + LAST + '\n', this.read.toString());
    }
}
