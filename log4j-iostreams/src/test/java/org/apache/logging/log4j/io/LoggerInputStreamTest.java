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
package org.apache.logging.log4j.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoggerInputStreamTest extends AbstractStreamTest {
    protected ByteArrayInputStream wrapped;
    protected ByteArrayOutputStream read;
    protected InputStream in;

    LoggerInputStreamTest(LoggerContext context) {
        super(context);
    }

    protected InputStream createInputStream() {
        return IoBuilder.forLogger(getExtendedLogger())
                .filter(this.wrapped)
                .setLevel(LEVEL)
                .buildInputStream();
    }

    @BeforeEach
    public void createStream() {
        this.wrapped = new ByteArrayInputStream((FIRST + "\r\n" + LAST).getBytes());
        this.read = new ByteArrayOutputStream();
        this.in = createInputStream();
    }

    @Test
    public void testClose_HasRemainingData() throws IOException {
        final byte[] bytes = new byte[1024];
        this.in.read(bytes);
        assertMessages(FIRST);
        this.in.close();
        assertMessages(FIRST, LAST);
    }

    @Test
    public void testClose_NoRemainingData() throws IOException {
        this.wrapped = new ByteArrayInputStream((FIRST + '\n').getBytes());
        this.in = createInputStream();

        final byte[] bytes = new byte[1024];
        this.in.read(bytes);
        assertMessages(FIRST);
        this.in.close();
        assertMessages(FIRST);
    }

    @Test
    public void testRead_ByteArray() throws Exception {
        final byte[] bytes = new byte[FIRST.length()];
        assertEquals(bytes.length, this.in.read(bytes), "len");
        if (!(this.in instanceof BufferedInputStream)) {
            assertMessages();
        }
        this.in.read(bytes);
        assertMessages(FIRST);
    }

    @Test
    public void testRead_ByteArray_Offset_Length() throws Exception {
        final byte[] bytes = new byte[FIRST.length() * 2];
        assertEquals(FIRST.length(), this.in.read(bytes, 0, FIRST.length()), "len");
        if (!(this.in instanceof BufferedInputStream)) {
            assertMessages();
        }
        this.in.read(bytes);
        assertMessages(FIRST);
    }

    @Test
    public void testRead_IgnoresWindowsNewline() throws IOException {
        final byte[] bytes = new byte[1024];
        final int len = this.in.read(bytes);
        this.read.write(bytes, 0, len);
        assertMessages(FIRST);
        assertEquals(FIRST + "\r\n" + LAST, this.read.toString());
        this.in.close();
        assertMessages(FIRST, LAST);
    }

    @Test
    public void testRead_int() throws Exception {
        for (int i = 0; i < FIRST.length(); i++) {
            this.read.write(this.in.read());
        }
        if (!(this.in instanceof BufferedInputStream)) {
            assertMessages();
        }
        assertEquals('\r', this.in.read(), "carriage return");
        if (!(this.in instanceof BufferedInputStream)) {
            assertMessages();
        }
        assertEquals('\n', this.in.read(), "newline");
        assertMessages(FIRST);
    }

    @Test
    public void testRead_MultipleLines() throws IOException {
        this.wrapped = new ByteArrayInputStream((FIRST + "\n" + LAST + '\n').getBytes());
        this.in = createInputStream();

        final byte[] bytes = new byte[1024];
        final int len = this.in.read(bytes);
        this.read.write(bytes, 0, len);
        assertMessages(FIRST, LAST);
        assertEquals(FIRST + '\n' + LAST + '\n', this.read.toString());
    }
}
