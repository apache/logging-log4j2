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
 * Test {@link BoundedFIFO}.
 *
 * @since 0.9.1
 */
public class BoundedFIFOTestCase extends TestCase {
    static Logger cat = Logger.getLogger("x");

    static int MAX = 1000;

    static LoggingEvent[] e = new LoggingEvent[MAX];

    public static Test suite() {
        final TestSuite suite = new TestSuite();
        suite.addTest(new BoundedFIFOTestCase("test1"));
        suite.addTest(new BoundedFIFOTestCase("test2"));
        suite.addTest(new BoundedFIFOTestCase("testResize1"));
        suite.addTest(new BoundedFIFOTestCase("testResize2"));
        suite.addTest(new BoundedFIFOTestCase("testResize3"));
        return suite;
    }

    {
        for (int i = 0; i < MAX; i++) {
            e[i] = new LoggingEvent("", cat, Level.DEBUG, "e" + i, null);
        }
    }

    public BoundedFIFOTestCase(final String name) {
        super(name);
    }

    int min(final int a, final int b) {
        return a < b ? a : b;
    }

    @Override
    public void setUp() {}

    /**
     * Pattern: +++++..-----..
     */
    public void test1() {
        for (int size = 1; size <= 128; size *= 2) {
            final BoundedFIFO bf = new BoundedFIFO(size);

            assertEquals(bf.getMaxSize(), size);
            assertNull(bf.get());

            int i;
            int j;
            int k;

            for (i = 1; i < 2 * size; i++) {
                for (j = 0; j < i; j++) {
                    // System.out.println("Putting "+e[j]);
                    bf.put(e[j]);
                    assertEquals(bf.length(), j < size ? j + 1 : size);
                }
                final int max = size < j ? size : j;
                j--;
                for (k = 0; k <= j; k++) {
                    // System.out.println("max="+max+", j="+j+", k="+k);
                    assertEquals(bf.length(), max - k > 0 ? max - k : 0);
                    final Object r = bf.get();
                    // System.out.println("Got "+r);
                    if (k >= size) {
                        assertNull(r);
                    } else {
                        assertEquals(r, e[k]);
                    }
                }
            }
            // System.out.println("Passed size="+size);
        }
    }

    /**
     * Pattern: ++++--++--++
     */
    public void test2() {
        final int size = 3;
        final BoundedFIFO bf = new BoundedFIFO(size);

        bf.put(e[0]);
        assertEquals(bf.get(), e[0]);
        assertNull(bf.get());

        bf.put(e[1]);
        assertEquals(bf.length(), 1);
        bf.put(e[2]);
        assertEquals(bf.length(), 2);
        bf.put(e[3]);
        assertEquals(bf.length(), 3);
        assertEquals(bf.get(), e[1]);
        assertEquals(bf.length(), 2);
        assertEquals(bf.get(), e[2]);
        assertEquals(bf.length(), 1);
        assertEquals(bf.get(), e[3]);
        assertEquals(bf.length(), 0);
        assertNull(bf.get());
        assertEquals(bf.length(), 0);
    }

    /**
     * Pattern ++++++++++++++++++++ (insert only);
     */
    public void testResize1() {
        final int size = 10;

        for (int n = 1; n < size * 2; n++) {
            for (int i = 0; i < size * 2; i++) {

                final BoundedFIFO bf = new BoundedFIFO(size);
                for (int f = 0; f < i; f++) {
                    bf.put(e[f]);
                }

                bf.resize(n);
                final int expectedSize = min(n, min(i, size));
                assertEquals(bf.length(), expectedSize);
                for (int c = 0; c < expectedSize; c++) {
                    assertEquals(bf.get(), e[c]);
                }
            }
        }
    }

    /**
     * Pattern ++...+ --...-
     */
    public void testResize2() {
        final int size = 10;

        for (int n = 1; n < size * 2; n++) {
            for (int i = 0; i < size * 2; i++) {
                for (int d = 0; d < min(i, size); d++) {

                    final BoundedFIFO bf = new BoundedFIFO(size);
                    for (int p = 0; p < i; p++) {
                        bf.put(e[p]);
                    }

                    for (int g = 0; g < d; g++) {
                        bf.get();
                    }

                    // x = the number of elems in
                    final int x = bf.length();

                    bf.resize(n);

                    final int expectedSize = min(n, x);
                    assertEquals(bf.length(), expectedSize);

                    for (int c = 0; c < expectedSize; c++) {
                        assertEquals(bf.get(), e[c + d]);
                    }
                    assertNull(bf.get());
                }
            }
        }
    }

    /**
     * Pattern: i inserts, d deletes, r inserts
     */
    public void testResize3() {
        final int size = 10;

        for (int n = 1; n < size * 2; n++) {
            for (int i = 0; i < size; i++) {
                for (int d = 0; d < i; d++) {
                    for (int r = 0; r < d; r++) {

                        final BoundedFIFO bf = new BoundedFIFO(size);
                        for (int p0 = 0; p0 < i; p0++) {
                            bf.put(e[p0]);
                        }

                        for (int g = 0; g < d; g++) {
                            bf.get();
                        }
                        for (int p1 = 0; p1 < r; p1++) {
                            bf.put(e[i + p1]);
                        }

                        final int x = bf.length();

                        bf.resize(n);

                        final int expectedSize = min(n, x);
                        assertEquals(bf.length(), expectedSize);

                        for (int c = 0; c < expectedSize; c++) {
                            assertEquals(bf.get(), e[c + d]);
                        }
                        // assertNull(bf.get());
                    }
                }
            }
        }
    }
}
