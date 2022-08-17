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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ThrowablesTest {

    @Test
    public void testGetRootCauseNone() {
        final NullPointerException throwable = new NullPointerException();
        assertEquals(throwable, Throwables.getRootCause(throwable));
    }

    @Test
    public void testGetRootCauseDepth1() {
        final Throwable cause = new NullPointerException();
        final Throwable error = new UnsupportedOperationException(cause);
        assertEquals(cause, Throwables.getRootCause(error));
    }

    @Test
    public void testGetRootCauseDepth2() {
        final Throwable rootCause = new NullPointerException();
        final Throwable cause = new UnsupportedOperationException(rootCause);
        final Throwable error = new IllegalArgumentException(cause);
        assertEquals(rootCause, Throwables.getRootCause(error));
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    public void testGetRootCauseLoop() {
        final Throwable cause1 = new RuntimeException();
        final Throwable cause2 = new RuntimeException(cause1);
        final Throwable cause3 = new RuntimeException(cause2);
        cause1.initCause(cause3);
        assertThrows(IllegalArgumentException.class, () -> Throwables.getRootCause(cause3));
    }

    @Test
    public void testRethrowRuntimeException() {
        assertThrows(NullPointerException.class, () -> Throwables.rethrow(new NullPointerException()));
    }

    @Test
    public void testRethrowError() {
        assertThrows(UnknownError.class, () -> Throwables.rethrow(new UnknownError()));
    }

    @Test
    public void testRethrowCheckedException() {
        assertThrows(NoSuchMethodException.class, () -> Throwables.rethrow(new NoSuchMethodException()));
    }
}
