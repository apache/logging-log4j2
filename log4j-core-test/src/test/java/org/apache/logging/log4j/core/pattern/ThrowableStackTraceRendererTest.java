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
package org.apache.logging.log4j.core.pattern;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class ThrowableStackTraceRendererTest {

    @Test
    void handlesConcurrentlyMutatedSuppressions() {
        final Throwable suppressed1 = new Throwable("suppressed1");
        final Throwable suppressed2 = new Throwable("suppressed2");
        final Throwable throwable = spy(new Throwable("throwable"));
        final ThrowableRenderer renderer =
                new ThrowableStackTraceRenderer<>(Collections.emptyList(), Integer.MAX_VALUE);
        // returning suppressed exception arrays of different lengths on each invocation to getSuppressed() emulates
        // the behavior experienced by ThrowableStackTraceRenderer when rendering a Throwable whose suppressed
        // exceptions are being concurrently mutated by another thread.
        doReturn(new Throwable[] {suppressed1})
                .doReturn(new Throwable[] {suppressed1, suppressed2})
                .when(throwable)
                .getSuppressed();
        // should not throw NPE
        renderer.renderThrowable(new StringBuilder(), throwable, "\n");
    }
}
