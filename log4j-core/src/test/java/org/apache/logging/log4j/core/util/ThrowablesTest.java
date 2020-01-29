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

import org.junit.Assert;
import org.junit.Test;

public class ThrowablesTest {

    @Test
    public void testGetRootCauseNone() {
        final NullPointerException throwable = new NullPointerException();
        Assert.assertEquals(throwable, Throwables.getRootCause(throwable));
    }

    @Test
    public void testGetRootCauseDepth1() {
        final Throwable cause = new NullPointerException();
        final Throwable error = new UnsupportedOperationException(cause);
        Assert.assertEquals(cause, Throwables.getRootCause(error));
    }

    @Test
    public void testGetRootCauseDepth2() {
        final Throwable rootCause = new NullPointerException();
        final Throwable cause = new UnsupportedOperationException(rootCause);
        final Throwable error = new IllegalArgumentException(cause);
        Assert.assertEquals(rootCause, Throwables.getRootCause(error));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetRootCauseLoop() {
        final Throwable cause1 = new RuntimeException();
        final Throwable cause2 = new RuntimeException(cause1);
        final Throwable cause3 = new RuntimeException(cause2);
        cause1.initCause(cause3);
        // noinspection ThrowableNotThrown
        Throwables.getRootCause(cause3);
    }

    @Test(expected = NullPointerException.class)
    public void testRethrowRuntimeException() {
        Throwables.rethrow(new NullPointerException());
    }

    @Test(expected = UnknownError.class)
    public void testRethrowError() {
        Throwables.rethrow(new UnknownError());
    }

    @Test(expected = NoSuchMethodException.class)
    public void testRethrowCheckedException() {
        Throwables.rethrow(new NoSuchMethodException());
    }
}
