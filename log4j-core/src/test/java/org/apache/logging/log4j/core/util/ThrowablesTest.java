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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ThrowablesTest {

    @Test
    public void testGetRootCauseNone() {
        final NullPointerException throwable = new NullPointerException();
        assertThat(Throwables.getRootCause(throwable)).isEqualTo(throwable);
    }

    @Test
    public void testGetRootCauseDepth1() {
        final Throwable cause = new NullPointerException();
        final Throwable error = new UnsupportedOperationException(cause);
        assertThat(Throwables.getRootCause(error)).isEqualTo(cause);
    }

    @Test
    public void testGetRootCauseDepth2() {
        final Throwable rootCause = new NullPointerException();
        final Throwable cause = new UnsupportedOperationException(rootCause);
        final Throwable error = new IllegalArgumentException(cause);
        assertThat(Throwables.getRootCause(error)).isEqualTo(rootCause);
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    public void testGetRootCauseLoop() {
        final Throwable cause1 = new RuntimeException();
        final Throwable cause2 = new RuntimeException(cause1);
        final Throwable cause3 = new RuntimeException(cause2);
        cause1.initCause(cause3);
        assertThatThrownBy(() -> Throwables.getRootCause(cause3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testRethrowRuntimeException() {
        assertThatThrownBy(() -> Throwables.rethrow(new NullPointerException())).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testRethrowError() {
        assertThatThrownBy(() -> Throwables.rethrow(new UnknownError())).isInstanceOf(UnknownError.class);
    }

    @Test
    public void testRethrowCheckedException() {
        assertThatThrownBy(() -> Throwables.rethrow(new NoSuchMethodException())).isInstanceOf(NoSuchMethodException.class);
    }
}
