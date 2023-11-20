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
package org.apache.log4j.spi;

import java.io.PrintWriter;
import junit.framework.TestCase;

/**
 * Tests {@link ThrowableInformation}.
 */
public class ThrowableInformationTest extends TestCase {

    /**
     * Create ThrowableInformationTest.
     *
     * @param name test name.
     */
    public ThrowableInformationTest(final String name) {
        super(name);
    }

    /**
     * Custom throwable that only calls methods overridden by VectorWriter in log4j 1.2.14 and earlier.
     */
    private static final class OverriddenThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        /**
         * Create new instance.
         */
        public OverriddenThrowable() {}

        /**
         * Print stack trace.
         *
         * @param s print writer.
         */
        public void printStackTrace(final PrintWriter s) {
            s.print((Object) "print(Object)");
            s.print("print(char[])".toCharArray());
            s.print("print(String)");
            s.println((Object) "println(Object)");
            s.println("println(char[])".toCharArray());
            s.println("println(String)");
            s.write("write(char[])".toCharArray());
            s.write("write(char[], int, int)".toCharArray(), 2, 8);
            s.write("write(String, int, int)", 2, 8);
        }
    }

    /**
     * Test capturing stack trace from a throwable that only uses the PrintWriter methods overridden in log4j 1.2.14 and
     * earlier.
     */
    public void testOverriddenBehavior() {
        final ThrowableInformation ti = new ThrowableInformation(new OverriddenThrowable());
        final String[] rep = ti.getThrowableStrRep();
        assertEquals(4, rep.length);
        assertEquals("print(Object)print(char[])print(String)println(Object)", rep[0]);
        assertEquals("println(char[])", rep[1]);
        assertEquals("println(String)", rep[2]);
        assertEquals("write(char[])ite(charite(Stri", rep[3]);
    }

    /**
     * Custom throwable that calls methods not overridden by VectorWriter in log4j 1.2.14 and earlier.
     */
    private static final class NotOverriddenThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        /**
         * Create new instance.
         */
        public NotOverriddenThrowable() {}

        /**
         * Print stack trace.
         *
         * @param s print writer.
         */
        public void printStackTrace(final PrintWriter s) {
            s.print(true);
            s.print('a');
            s.print(1);
            s.print(2L);
            s.print(Float.MAX_VALUE);
            s.print(Double.MIN_VALUE);
            s.println(true);
            s.println('a');
            s.println(1);
            s.println(2L);
            s.println(Float.MAX_VALUE);
            s.println(Double.MIN_VALUE);
            s.write('C');
        }
    }

    /**
     * Test capturing stack trace from a throwable that uses the PrintWriter methods not overridden in log4j 1.2.14 and
     * earlier.
     */
    public void testNotOverriddenBehavior() {
        final ThrowableInformation ti = new ThrowableInformation(new NotOverriddenThrowable());
        final String[] rep = ti.getThrowableStrRep();
        assertEquals(7, rep.length);
        final StringBuffer buf = new StringBuffer(String.valueOf(true));
        buf.append('a');
        buf.append(String.valueOf(1));
        buf.append(String.valueOf(2L));
        buf.append(String.valueOf(Float.MAX_VALUE));
        buf.append(String.valueOf(Double.MIN_VALUE));
        buf.append(String.valueOf(true));
        assertEquals(buf.toString(), rep[0]);
        assertEquals("a", rep[1]);
        assertEquals(String.valueOf(1), rep[2]);
        assertEquals(String.valueOf(2L), rep[3]);
        assertEquals(String.valueOf(Float.MAX_VALUE), rep[4]);
        assertEquals(String.valueOf(Double.MIN_VALUE), rep[5]);
        assertEquals("C", rep[6]);
    }

    /**
     * Custom throwable that calls methods of VectorWriter with null.
     */
    private static final class NullThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        /**
         * Create new instance.
         */
        public NullThrowable() {}

        /**
         * Print stack trace.
         *
         * @param s print writer.
         */
        public void printStackTrace(final PrintWriter s) {
            s.print((Object) null);
            s.print((String) null);
            s.println((Object) null);
            s.println((String) null);
        }
    }

    /**
     * Test capturing stack trace from a throwable that passes null to PrintWriter methods.
     */
    public void testNull() {
        final ThrowableInformation ti = new ThrowableInformation(new NullThrowable());
        final String[] rep = ti.getThrowableStrRep();
        assertEquals(2, rep.length);
        final String nullStr = String.valueOf((Object) null);
        assertEquals(nullStr + nullStr + nullStr, rep[0]);
        assertEquals(nullStr, rep[1]);
    }

    /**
     * Custom throwable that does nothing in printStackTrace.
     */
    private static final class EmptyThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        /**
         * Create new instance.
         */
        public EmptyThrowable() {}

        /**
         * Print stack trace.
         *
         * @param s print writer.
         */
        public void printStackTrace(final PrintWriter s) {}
    }

    /**
     * Test capturing stack trace from a throwable that does nothing on a call to printStackTrace.
     */
    public void testEmpty() {
        final ThrowableInformation ti = new ThrowableInformation(new EmptyThrowable());
        final String[] rep = ti.getThrowableStrRep();
        assertEquals(0, rep.length);
    }

    /**
     * Custom throwable that emits a specified string in printStackTrace.
     */
    private static final class StringThrowable extends Throwable {
        private static final long serialVersionUID = 1L;
        /**
         * Stack trace.
         */
        private final String stackTrace;

        /**
         * Create new instance.
         *
         * @param trace stack trace.
         */
        public StringThrowable(final String trace) {
            stackTrace = trace;
        }

        /**
         * Print stack trace.
         *
         * @param s print writer.
         */
        public void printStackTrace(final PrintWriter s) {
            s.print(stackTrace);
        }
    }

    /**
     * Test capturing stack trace from throwable that just has a line feed.
     */
    public void testLineFeed() {
        final ThrowableInformation ti = new ThrowableInformation(new StringThrowable("\n"));
        final String[] rep = ti.getThrowableStrRep();
        assertEquals(1, rep.length);
        assertEquals("", rep[0]);
    }

    /**
     * Test capturing stack trace from throwable that just has a carriage return.
     */
    public void testCarriageReturn() {
        final ThrowableInformation ti = new ThrowableInformation(new StringThrowable("\r"));
        final String[] rep = ti.getThrowableStrRep();
        assertEquals(1, rep.length);
        assertEquals("", rep[0]);
    }

    /**
     * Test parsing of line breaks.
     */
    public void testParsing() {
        final ThrowableInformation ti =
                new ThrowableInformation(new StringThrowable("Line1\rLine2\nLine3\r\nLine4\n\rLine6"));
        final String[] rep = ti.getThrowableStrRep();
        assertEquals(6, rep.length);
        assertEquals("Line1", rep[0]);
        assertEquals("Line2", rep[1]);
        assertEquals("Line3", rep[2]);
        assertEquals("Line4", rep[3]);
        assertEquals("", rep[4]);
        assertEquals("Line6", rep[5]);
    }

    /**
     * Test capturing stack trace from throwable that a line feed followed by blank.
     */
    public void testLineFeedBlank() {
        final ThrowableInformation ti = new ThrowableInformation(new StringThrowable("\n "));
        final String[] rep = ti.getThrowableStrRep();
        assertEquals(2, rep.length);
        assertEquals("", rep[0]);
        assertEquals(" ", rep[1]);
    }

    /**
     * Test that getThrowable returns the throwable provided to the constructor.
     */
    public void testGetThrowable() {
        final Throwable t = new StringThrowable("Hello, World");
        final ThrowableInformation ti = new ThrowableInformation(t);
        assertSame(t, ti.getThrowable());
    }

    /**
     * Tests isolation of returned string representation from internal state of ThrowableInformation. log4j 1.2.15 and
     * earlier did not isolate initial call. See bug 44032.
     */
    public void testIsolation() {
        final ThrowableInformation ti = new ThrowableInformation(new StringThrowable("Hello, World"));
        final String[] rep = ti.getThrowableStrRep();
        assertEquals("Hello, World", rep[0]);
        rep[0] = "Bonjour, Monde";
        final String[] rep2 = ti.getThrowableStrRep();
        assertEquals("Hello, World", rep2[0]);
    }

    /**
     * Custom throwable that throws a runtime exception when printStackTrace is called.
     */
    private static final class NastyThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        /**
         * Create new instance.
         */
        public NastyThrowable() {}

        /**
         * Print stack trace.
         *
         * @param s print writer.
         */
        public void printStackTrace(final PrintWriter s) {
            s.print("NastyException");
            throw new RuntimeException("Intentional exception");
        }
    }

    /**
     * Tests that a failure in printStackTrace does not percolate out of getThrowableStrRep().
     *
     */
    public void testNastyException() {
        final ThrowableInformation ti = new ThrowableInformation(new NastyThrowable());
        final String[] rep = ti.getThrowableStrRep();
        assertEquals("NastyException", rep[0]);
    }
}
