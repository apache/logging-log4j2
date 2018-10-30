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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

public abstract class AbstractLoggerOutputStreamTest extends AbstractStreamTest {
    
    protected OutputStream out;
    protected ByteArrayOutputStream wrapped;
    protected abstract ByteArrayOutputStream createOutputStream();
    protected abstract OutputStream createOutputStreamWrapper();

    @Before
    public void createStream() {
        this.wrapped = createOutputStream();
        this.out = createOutputStreamWrapper();
    }

    @Test
    public void testClose_HasRemainingData() throws IOException {
        this.out.write(FIRST.getBytes());
        assertMessages();
        this.out.close();
        assertMessages(FIRST);
        if (this.wrapped != null) {
            assertEquals(FIRST, this.wrapped.toString());
        }
    }

    @Test
    public void testClose_NoRemainingData() throws IOException {
        this.out.close();
        assertMessages();
        if (this.wrapped != null) {
            assertEquals("", this.wrapped.toString());
        }
    }

    @Test
    public void testFlush() throws IOException {
        final OutputStream os = mock(OutputStream.class);

        try (final OutputStream filteredOut =
            IoBuilder.forLogger(getExtendedLogger())
                .filter(os)
                .setLevel(LEVEL)
                .buildOutputStream()) {
          filteredOut.flush();
        }

        then(os).should().flush();
        then(os).should().close();
        then(os).shouldHaveNoMoreInteractions();
    }

    @Test
    public void testWrite_ByteArray() throws Exception {
        final byte[] bytes = "byte[]".getBytes();
        this.out.write(bytes);
        assertMessages();
        this.out.write('\n');
        assertMessages("byte[]");
        if (this.wrapped != null) {
            assertEquals("byte[]\n", this.wrapped.toString());
        }
    }

    @Test
    public void testWrite_ByteArray_Offset_Length() throws Exception {
        final byte[] bytes = "byte[]".getBytes();
        final int middle = bytes.length / 2;
        final int length = bytes.length - middle;
        final String right = new String(bytes, middle, length);
        this.out.write(bytes, middle, length);
        assertMessages();
        this.out.write('\n');
        assertMessages(right);
        if (this.wrapped != null) {
            assertEquals("byte[]".substring(middle, bytes.length) + '\n', this.wrapped.toString());
        }
    }

    @Test
    public void testWrite_IgnoresWindowsNewline() throws IOException {
        this.out.write(FIRST.getBytes());
        this.out.write("\r\n".getBytes());
        this.out.write(LAST.getBytes());
        this.out.close();
        assertMessages(FIRST, LAST);
        if (this.wrapped != null) {
            assertEquals(FIRST + "\r\n" + LAST, this.wrapped.toString());
        }
    }

    @Test
    public void testWrite_Int() throws Exception {
        for (final byte b : "int".getBytes()) {
            this.out.write(b);
            assertMessages();
        }
        this.out.write('\n');
        assertMessages("int");
        if (this.wrapped != null) {
            assertEquals("int" + '\n', this.wrapped.toString());
        }
    }

    @Test
    public void testWrite_MultipleLines() throws IOException {
        this.out.write((FIRST + '\n' + LAST + '\n').getBytes());
        assertMessages(FIRST, LAST);
        if (this.wrapped != null) {
            assertEquals(FIRST + '\n' + LAST + '\n', this.wrapped.toString());
        }
    }
}
