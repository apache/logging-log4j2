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

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.logging.log4j.Level;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

public class LoggerOutputStreamTest extends AbstractStreamTest {
    protected ByteArrayOutputStream wrapped;
    protected OutputStream out;

    protected OutputStream createOutputStream() {
        return new LoggerOutputStream(this.wrapped, getLogger(), Level.ERROR);
    }

    @Before
    public void createStream() {
        this.wrapped = new ByteArrayOutputStream();
        this.out = createOutputStream();
    }

    @Test
    public void testClose_HasRemainingData() throws IOException {
        this.out.write(FIRST.getBytes());
        assertMessages();
        this.out.close();
        assertMessages(FIRST);
        assertEquals(FIRST, this.wrapped.toString());
    }

    @Test
    public void testClose_NoRemainingData() throws IOException {
        this.out.close();
        assertMessages();
        assertEquals("", this.wrapped.toString());
    }

    @Test
    public void testFlush() throws IOException {
        final OutputStream out = EasyMock.createMock("out", OutputStream.class);
        out.flush(); // expect the flush to come through to the mocked OutputStream
        out.close();
        replay(out);
        
        final LoggerOutputStream los = new LoggerOutputStream(out, getLogger(), LEVEL);
        los.flush();
        los.close();
        verify(out);
    }

    @Test
    public void testWrite_ByteArray() throws Exception {
        final byte[] bytes = "byte[]".getBytes();
        this.out.write(bytes);
        assertMessages();
        this.out.write('\n');
        assertMessages("byte[]");
        assertEquals("byte[]\n", this.wrapped.toString());
    }

    @Test
    public void testWrite_ByteArray_Offset_Length() throws Exception {
        final byte[] bytes = "byte[]".getBytes();
        final int middle = bytes.length/2;
        final int length = bytes.length - middle;
        final String right = new String(bytes, middle, length);
        this.out.write(bytes, middle, length);
        assertMessages();
        this.out.write('\n');
        assertMessages(right);
        assertEquals("byte[]".substring(middle, bytes.length) + '\n', this.wrapped.toString());
    }

    @Test
    public void testWrite_IgnoresWindowsNewline() throws IOException {
        this.out.write(FIRST.getBytes());
        this.out.write("\r\n".getBytes());
        this.out.write(LAST.getBytes());
        this.out.close();
        assertMessages(FIRST, LAST);
        assertEquals(FIRST + "\r\n" + LAST, this.wrapped.toString());
    }

    @Test
    public void testWrite_Int() throws Exception {
        for (final byte b : "int".getBytes()) {
            this.out.write(b);
            assertMessages();
        }
        this.out.write('\n');
        assertMessages("int");
        assertEquals("int" + '\n', this.wrapped.toString());
    }

    @Test
    public void testWrite_MultipleLines() throws IOException {
        this.out.write((FIRST + '\n' + LAST + '\n').getBytes());
        assertMessages(FIRST, LAST);
        assertEquals(FIRST + '\n' + LAST + '\n', this.wrapped.toString());
    }
}
