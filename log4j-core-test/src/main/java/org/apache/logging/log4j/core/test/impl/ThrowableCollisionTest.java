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
package org.apache.logging.log4j.core.test.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.junit.jupiter.api.Test;

public class ThrowableCollisionTest {

    static class CollidingException extends RuntimeException {
        public CollidingException(String message, Throwable cause) {
            super(message, cause);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CollidingException
                    && Objects.equals(getMessage(), ((CollidingException) obj).getMessage());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getMessage());
        }
    }

    static class CyclicException extends RuntimeException {
        private Throwable customCause;

        public CyclicException(String message) {
            super(message);
        }

        public void setCustomCause(Throwable cause) {
            this.customCause = cause;
        }

        @Override
        public Throwable getCause() {
            return customCause;
        }
    }

    @Test
    public void testCollisionDoesNotTriggerCircularReference() {
        Throwable inner = new CollidingException("collision", null);
        Throwable outer = new CollidingException("collision", inner);

        assertDoesNotThrow(() -> {
            ThrowableProxy proxy = new ThrowableProxy(outer);
            String trace = proxy.getExtendedStackTraceAsString();

            assertFalse(
                    trace.contains("CIRCULAR REFERENCE"),
                    "Should not mark a non-cyclic colliding exception chain as circular!");
        });
    }

    @Test
    public void testTrueCircularReferenceIsStillHandledSafely() {
        CyclicException ex1 = new CyclicException("Cycle Exception 1");
        CyclicException ex2 = new CyclicException("Cycle Exception 2");

        ex1.setCustomCause(ex2);
        ex2.setCustomCause(ex1);

        assertDoesNotThrow(() -> {
            ThrowableProxy proxy = new ThrowableProxy(ex1);
            String trace = proxy.getExtendedStackTraceAsString();

            assertTrue(
                    trace.contains("CIRCULAR REFERENCE"),
                    "Should successfully detect and flag a genuine cyclic reference!");
        });
    }
}
