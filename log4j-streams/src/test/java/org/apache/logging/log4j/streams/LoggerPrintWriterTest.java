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
import static org.junit.Assert.assertSame;

import java.io.PrintWriter;
import java.io.Writer;

import org.junit.Test;

public class LoggerPrintWriterTest extends LoggerWriterTest {
    private PrintWriter print; 

    @Override
    protected Writer createWriter() {
        this.print = new LoggerPrintWriter(wrapped, getLogger(), LEVEL);
        return this.print;
    }

    @Test
    public void testPrint_boolean() throws Exception {
        print.print(true);
        assertMessages();
        print.println();
        assertMessages("true");
        assertEquals("true" + NEWLINE, wrapped.toString());
    }

    @Test
    public void testPrint_char() throws Exception {
        for (char c : FIRST.toCharArray()) {
            print.print(c);
            assertMessages();
        }
        print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, wrapped.toString());
    }

    @Test
    public void testPrint_int() throws Exception {
        print.print(12);
        assertMessages();
        print.println();
        assertMessages("12");
        assertEquals("12" + NEWLINE, wrapped.toString());
    }

    @Test
    public void testPrint_long() throws Exception {
        print.print(12L);
        assertMessages();
        print.println();
        assertMessages("12");
        assertEquals("12" + NEWLINE, wrapped.toString());
    }

    @Test
    public void testPrint_CharacterArray() throws Exception {
        print.print(FIRST.toCharArray());
        assertMessages();
        print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, wrapped.toString());
    }

    @Test
    public void testPrint_String() throws Exception {
        print.print(FIRST);
        assertMessages();
        print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, wrapped.toString());
    }

    @Test
    public void testPrint_Object() throws Exception {
        print.print((Object) FIRST);
        assertMessages();
        print.println();
        assertMessages(FIRST);
        assertEquals(FIRST + NEWLINE, wrapped.toString());
    }

    @Test
    public void testPrintf() throws Exception {
        assertSame(print,  print.printf("<<<%s>>>", FIRST));
        assertMessages();
        print.println();
        assertMessages("<<<" + FIRST + ">>>");
        assertEquals("<<<" + FIRST + ">>>" + NEWLINE, wrapped.toString());
    }

    @Test
    public void testFormat() throws Exception {
        assertSame(print, print.format("[%s]", FIRST));
        assertMessages();
        print.println();
        assertMessages("[" + FIRST + "]");
        assertEquals("[" + FIRST + "]" + NEWLINE, wrapped.toString());
    }
}
