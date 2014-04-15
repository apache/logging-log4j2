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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

public class LoggerWriterTest extends StreamTesting {
    protected StringWriter wrapped;
    protected Writer writer;

    @Before
    public void createStream() {
        wrapped = new StringWriter();
        writer = createWriter();
    }

    protected Writer createWriter() {
        return new LoggerWriter(wrapped, getLogger(), LEVEL);
    }

    @Test
    public void testWrite_CharArray() throws Exception {
        final char[] chars = FIRST.toCharArray();
        writer.write(chars);
        assertMessages();
        writer.write('\n');
        assertMessages(FIRST);
        assertEquals(FIRST + '\n', wrapped.toString());
    }

    @Test
    public void testWrite_CharArray_Offset_Length() throws Exception {
        final char[] chars = FIRST.toCharArray();
        int middle = chars.length / 2;
        int length = chars.length - middle;
        final String right = new String(chars, middle, length);
        writer.write(chars, middle, length);
        assertMessages();
        writer.write('\n');
        assertMessages(right);
        assertEquals(FIRST.substring(middle, FIRST.length()) + '\n', wrapped.toString());
    }

    @Test
    public void testWrite_Character() throws Exception {
        for (char c : FIRST.toCharArray()) {
            writer.write(c);
            assertMessages();
        }
        writer.write('\n');
        assertMessages(FIRST);
        assertEquals(FIRST + '\n', wrapped.toString());
    }

    @Test
    public void testWrite_IgnoresWindowsNewline() throws IOException {
        writer.write(FIRST + "\r\n");
        writer.write(LAST);
        writer.close();
        assertMessages(FIRST, LAST);
        assertEquals(FIRST + "\r\n" + LAST, wrapped.toString());
    }

    @Test
    public void testWrite_MultipleLines() throws IOException {
        writer.write(FIRST + '\n' + LAST + '\n');
        assertMessages(FIRST, LAST);
        assertEquals(FIRST + '\n' + LAST + '\n', wrapped.toString());
    }

    @Test
    public void testFlush() throws IOException {
        final OutputStream out = EasyMock.createMock(OutputStream.class);
        out.flush(); // expect the flush to come through to the mocked OutputStream
        out.close();
        replay(out);

        final LoggerOutputStream los = new LoggerOutputStream(out, getLogger(), LEVEL);
        los.flush();
        los.close();
        verify(out);
    }

    @Test
    public void testClose_NoRemainingData() throws IOException {
        writer.close();
        assertMessages();
        assertEquals("", wrapped.toString());
    }

    @Test
    public void testClose_HasRemainingData() throws IOException {
        writer.write(FIRST);
        assertMessages();
        writer.close();
        assertMessages(FIRST);
        assertEquals(FIRST, wrapped.toString());
    }
}
