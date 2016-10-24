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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

public abstract class AbstractLoggerWriterTest extends AbstractStreamTest {
    protected StringWriter wrapped;
    protected Writer writer;

    @Before
    public void createStream() {
        this.wrapped = createWriter();
        this.writer = createWriterWrapper();
    }

    protected abstract StringWriter createWriter();

    protected abstract Writer createWriterWrapper();

    @Test
    public void testClose_HasRemainingData() throws IOException {
        this.writer.write(FIRST);
        assertMessages();
        this.writer.close();
        assertMessages(FIRST);
        if (this.wrapped != null) {
            assertEquals(FIRST, this.wrapped.toString());
        }
    }

    @Test
    public void testClose_NoRemainingData() throws IOException {
        this.writer.close();
        assertMessages();
        if (this.wrapped != null) {
            assertEquals("", this.wrapped.toString());
        }
    }

    @Test
    public void testFlush() throws IOException {
        final OutputStream out = mock(OutputStream.class);

        try (final OutputStream filteredOut =
            IoBuilder.forLogger(getExtendedLogger())
                .filter(out)
                .setLevel(LEVEL)
                .buildOutputStream()) {
        	filteredOut.flush();
        }

        then(out).should().flush();
        then(out).should().close();
        then(out).shouldHaveNoMoreInteractions();
    }

    @Test
    public void testWrite_Character() throws Exception {
        for (final char c : FIRST.toCharArray()) {
            this.writer.write(c);
            assertMessages();
        }
        this.writer.write('\n');
        assertMessages(FIRST);
        if (this.wrapped != null) {
            assertEquals(FIRST + '\n', this.wrapped.toString());
        }
    }

    @Test
    public void testWrite_CharArray() throws Exception {
        final char[] chars = FIRST.toCharArray();
        this.writer.write(chars);
        assertMessages();
        this.writer.write('\n');
        assertMessages(FIRST);
        if (this.wrapped != null) {
            assertEquals(FIRST + '\n', this.wrapped.toString());
        }
    }

    @Test
    public void testWrite_CharArray_Offset_Length() throws Exception {
        final char[] chars = FIRST.toCharArray();
        final int middle = chars.length / 2;
        final int length = chars.length - middle;
        final String right = new String(chars, middle, length);
        this.writer.write(chars, middle, length);
        assertMessages();
        this.writer.write('\n');
        assertMessages(right);
        if (this.wrapped != null) {
            assertEquals(FIRST.substring(middle, FIRST.length()) + '\n', this.wrapped.toString());
        }
    }

    @Test
    public void testWrite_IgnoresWindowsNewline() throws IOException {
        this.writer.write(FIRST + "\r\n");
        this.writer.write(LAST);
        this.writer.close();
        assertMessages(FIRST, LAST);
        if (this.wrapped != null) {
            assertEquals(FIRST + "\r\n" + LAST, this.wrapped.toString());
        }
    }

    @Test
    public void testWrite_MultipleLines() throws IOException {
        this.writer.write(FIRST + '\n' + LAST + '\n');
        assertMessages(FIRST, LAST);
        if (this.wrapped != null) {
            assertEquals(FIRST + '\n' + LAST + '\n', this.wrapped.toString());
        }
    }
}
