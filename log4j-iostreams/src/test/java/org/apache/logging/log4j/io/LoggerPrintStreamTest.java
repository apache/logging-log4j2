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
import java.io.OutputStream;
import java.io.PrintStream;

import org.junit.Test;

import static org.junit.Assert.*;

public class LoggerPrintStreamTest extends AbstractLoggerOutputStreamTest {
    private PrintStream print;

    @Override
    protected ByteArrayOutputStream createOutputStream() {
        return new ByteArrayOutputStream();
    }

    @Override
    protected OutputStream createOutputStreamWrapper() {
        return this.print = IoBuilder.forLogger(getExtendedLogger())
            .filter(this.wrapped)
            .setLevel(LEVEL)
            .buildPrintStream();
    }

    @Test
    public void testFormat() throws Exception {
        assertSame(this.print, this.print.format("[%s]", FIRST));
        assertMessages();
        this.print.println();
        assertMessages("[" + FIRST + "]");
        assertEquals("[" + FIRST + "]" + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_boolean() throws Exception {
        this.print.print(true);
        assertMessages();
        this.print.println();
        assertMessages("true");
        assertEquals("true" + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_char() throws Exception {
        for (final char c : FIRST.toCharArray()) {
            this.print.print(c);
            assertMessages();
        }
        this.print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_CharacterArray() throws Exception {
        this.print.print(FIRST.toCharArray());
        assertMessages();
        this.print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_int() throws Exception {
        this.print.print(12);
        assertMessages();
        this.print.println();
        assertMessages("12");
        assertEquals("12" + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_long() throws Exception {
        this.print.print(12L);
        assertMessages();
        this.print.println();
        assertMessages("12");
        assertEquals("12" + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_Object() throws Exception {
        this.print.print((Object) FIRST);
        assertMessages();
        this.print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_String() throws Exception {
        this.print.print(FIRST);
        assertMessages();
        this.print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrintf() throws Exception {
        assertSame(this.print,  this.print.printf("<<<%s>>>", FIRST));
        assertMessages();
        this.print.println();
        assertMessages("<<<" + FIRST + ">>>");
        assertEquals("<<<" + FIRST + ">>>" + NEWLINE, this.wrapped.toString());
    }
}
