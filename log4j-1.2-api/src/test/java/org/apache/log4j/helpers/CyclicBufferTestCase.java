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
package org.apache.log4j.helpers;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Tests {@link CyclicBuffer}.
 */
public class CyclicBufferTestCase extends TestCase {

    static Logger cat = Logger.getLogger("x");

    static int MAX = 1000;

    static LoggingEvent[] e = new LoggingEvent[MAX];

    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTest(new CyclicBufferTestCase("test0"));
        suite.addTest(new CyclicBufferTestCase("test1"));
        suite.addTest(new CyclicBufferTestCase("testResize"));
        return suite;
    }

    {
        for (int i = 0; i < MAX; i++) {
            e[i] = new LoggingEvent("", cat, Level.DEBUG, "e" + i, null);
        }
    }

    public CyclicBufferTestCase(final String name) {
        super(name);
    }

    void doTest1(final int size) {
        // System.out.println("Doing test with size = "+size);
        final CyclicBuffer cb = new CyclicBuffer(size);

        assertEquals(cb.getMaxSize(), size);

        for (int i = -(size + 10); i < (size + 10); i++) {
            assertNull(cb.get(i));
        }

        for (int i = 0; i < MAX; i++) {
            cb.add(e[i]);
            final int limit = i < size - 1 ? i : size - 1;

            // System.out.println("\nLimit is " + limit + ", i="+i);

            for (int j = limit; j >= 0; j--) {
                // System.out.println("i= "+i+", j="+j);
                assertEquals(cb.get(j), e[i - (limit - j)]);
            }
            assertNull(cb.get(-1));
            assertNull(cb.get(limit + 1));
        }
    }

    void doTestResize(final int initialSize, final int numberOfAdds, final int newSize) {
        // System.out.println("initialSize = "+initialSize+", numberOfAdds="
        // +numberOfAdds+", newSize="+newSize);
        final CyclicBuffer cb = new CyclicBuffer(initialSize);
        for (int i = 0; i < numberOfAdds; i++) {
            cb.add(e[i]);
        }
        cb.resize(newSize);

        int offset = numberOfAdds - initialSize;
        if (offset < 0) {
            offset = 0;
        }

        int len = newSize < numberOfAdds ? newSize : numberOfAdds;
        len = len < initialSize ? len : initialSize;
        // System.out.println("Len = "+len+", offset="+offset);
        for (int j = 0; j < len; j++) {
            assertEquals(cb.get(j), e[offset + j]);
        }
    }

    @Override
    public void setUp() {}

    public void test0() {
        final int size = 2;

        CyclicBuffer cb = new CyclicBuffer(size);
        assertEquals(cb.getMaxSize(), size);

        cb.add(e[0]);
        assertEquals(cb.length(), 1);
        assertEquals(cb.get(), e[0]);
        assertEquals(cb.length(), 0);
        assertNull(cb.get());
        assertEquals(cb.length(), 0);

        cb = new CyclicBuffer(size);
        cb.add(e[0]);
        cb.add(e[1]);
        assertEquals(cb.length(), 2);
        assertEquals(cb.get(), e[0]);
        assertEquals(cb.length(), 1);
        assertEquals(cb.get(), e[1]);
        assertEquals(cb.length(), 0);
        assertNull(cb.get());
        assertEquals(cb.length(), 0);
    }

    /**
     * Test a buffer of size 1,2,4,8,..,128
     */
    public void test1() {
        for (int bufSize = 1; bufSize <= 128; bufSize *= 2) {
            doTest1(bufSize);
        }
    }

    public void testResize() {
        for (int isize = 1; isize <= 128; isize *= 2) {
            doTestResize(isize, isize / 2 + 1, isize / 2 + 1);
            doTestResize(isize, isize / 2 + 1, isize + 10);
            doTestResize(isize, isize + 10, isize / 2 + 1);
            doTestResize(isize, isize + 10, isize + 10);
        }
    }
}
