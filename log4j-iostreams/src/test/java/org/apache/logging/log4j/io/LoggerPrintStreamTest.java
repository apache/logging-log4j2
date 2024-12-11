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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Test;

public class LoggerPrintStreamTest extends AbstractLoggerOutputStreamTest {
    private PrintStream print;

    LoggerPrintStreamTest(LoggerContext context) {
        super(context);
    }

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
    public void testFormat() {
        assertSame(this.print, this.print.format("[%s]", FIRST));
        assertMessages();
        this.print.println();
        assertMessages("[" + FIRST + "]");
        assertEquals("[" + FIRST + "]" + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_boolean() {
        this.print.print(true);
        assertMessages();
        this.print.println();
        assertMessages("true");
        assertEquals("true" + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_char() {
        for (final char c : FIRST.toCharArray()) {
            this.print.print(c);
            assertMessages();
        }
        this.print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_CharacterArray() {
        this.print.print(FIRST.toCharArray());
        assertMessages();
        this.print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_int() {
        this.print.print(12);
        assertMessages();
        this.print.println();
        assertMessages("12");
        assertEquals("12" + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_long() {
        this.print.print(12L);
        assertMessages();
        this.print.println();
        assertMessages("12");
        assertEquals("12" + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_Object() {
        this.print.print((Object) FIRST);
        assertMessages();
        this.print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrint_String() {
        this.print.print(FIRST);
        assertMessages();
        this.print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, this.wrapped.toString());
    }

    @Test
    public void testPrintf() {
        assertSame(this.print, this.print.printf("<<<%s>>>", FIRST));
        assertMessages();
        this.print.println();
        assertMessages("<<<" + FIRST + ">>>");
        assertEquals("<<<" + FIRST + ">>>" + NEWLINE, this.wrapped.toString());
    }
}
