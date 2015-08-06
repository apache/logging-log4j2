/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.util;

import java.util.concurrent.Callable;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the LambdaUtil class.
 */
public class LambdaUtilTest {

    @Test
    public void testCallReturnsResultOfCallable() {
        final String expected = "result";
        final Object actual = LambdaUtil.call(new Callable<String>() {
            public String call() {
                return expected;
            }
        });
        assertSame(expected, actual);
    }

    @Test
    public void testCallReturnsNullIfCallableNull() {
        final Object actual = LambdaUtil.call(null);
        assertNull(actual);
    }

    @Test
    public void testCallReturnsExceptionIfCallableThrowsException() {
        final Exception expected = new RuntimeException();
        final Object actual = LambdaUtil.call(new Callable<String>() {
            public String call() throws Exception{
                throw expected;
            }
        });
        assertSame(expected, actual);
    }


    @Test
    public void testCallAllReturnsResultOfCallables() {
        final String expected1 = "result1";
        Callable<String> function1 = new Callable<String>() {
            public String call() {
                return expected1;
            }
        };
        final String expected2 = "result2";
        Callable<String> function2 = new Callable<String>() {
            public String call() {
                return expected2;
            }
        };
        
        Callable<?>[] functions = {function1, function2};
        final Object[] actual = LambdaUtil.callAll(functions);
        assertEquals(actual.length, functions.length);
        assertSame(expected1, actual[0]);
        assertSame(expected2, actual[1]);
    }

    @Test
    public void testCallAllReturnsNullArrayIfCallablesArrayNull() {
        final Object[] actual = LambdaUtil.callAll((Callable<?>[]) null);
        assertNull(actual);
    }

    @Test
    public void testCallAllReturnsNullElementsIfCallableArrayContainsNulls() {
        final Callable<?>[] functions = new Callable[3];
        final Object[] actual = LambdaUtil.callAll(functions);
        assertEquals(actual.length, functions.length);
        for (Object object : actual) {
            assertNull(object);
        }
    }

    @Test
    public void testCallAllReturnsExceptionsIfCallablesThrowsException() {
        final Exception expected1 = new RuntimeException();
        Callable<String> function1 = new Callable<String>() {
            public String call() throws Exception{
                throw expected1;
            }
        };
        final Exception expected2 = new RuntimeException();
        Callable<String> function2 = new Callable<String>() {
            public String call() throws Exception{
                throw expected2;
            }
        };
        
        Callable<?>[] functions = {function1, function2};
        final Object[] actual = LambdaUtil.callAll(functions);
        assertEquals(actual.length, functions.length);
        assertSame(expected1, actual[0]);
        assertSame(expected2, actual[1]);
    }
}
