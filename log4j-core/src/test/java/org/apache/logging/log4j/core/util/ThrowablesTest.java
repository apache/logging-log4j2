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
package org.apache.logging.log4j.core.util;

import org.junit.Test;

public class ThrowablesTest {

    @Test
    public void testGetRootCauseNone() throws Exception {
        final NullPointerException throwable = new NullPointerException();
        org.junit.Assert.assertEquals(throwable, Throwables.getRootCause(throwable));
    }

    @Test
    public void testGetRootCauseDepth1() throws Exception {
        final NullPointerException throwable = new NullPointerException();
        org.junit.Assert.assertEquals(throwable, Throwables.getRootCause(new UnsupportedOperationException(throwable)));
    }

    @Test
    public void testGetRootCauseDepth2() throws Exception {
        final NullPointerException throwable = new NullPointerException();
        org.junit.Assert.assertEquals(throwable,
                Throwables.getRootCause(new IllegalArgumentException(new UnsupportedOperationException(throwable))));
    }

    @Test(expected = NullPointerException.class)
    public void testRethrowRuntimeException() throws Exception {
        Throwables.rethrow(new NullPointerException());
    }

    @Test(expected = UnknownError.class)
    public void testRethrowError() throws Exception {
        Throwables.rethrow(new UnknownError());
    }

    @Test(expected = NoSuchMethodException.class)
    public void testRethrowCheckedException() throws Exception {
        Throwables.rethrow(new NoSuchMethodException());
    }
}
