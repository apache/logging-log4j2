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

/**
 * Utility class providing common validation logic.
 */
public final class Assert {
    private Assert() {
    }

    /**
     * Throws a {@code NullPointerException} if the specified parameter is
     * {@code null}, otherwise returns the specified parameter.
     * <p>
     * On Java 7, just use {@code Objects.requireNonNull(T, String)}
     * </p>
     * <p>
     * Usage:
     * </p>
     * <pre>
     * // earlier you would write this:
     * public SomeConstructor(Object param) {
     *     if (param == null) {
     *         throw new NullPointerException(&quot;param&quot;);
     *     }
     *     this.field = param;
     * }
     *
     * // now you can do the same in one line:
     * public SomeConstructor(Object param) {
     *     this.field = Assert.requireNonNull(&quot;param&quot;);
     * }
     * </pre>
     *
     * @param <T> the type of the parameter to check and return
     * @param object the parameter to check
     * @param message message to populate the NPE with if necessary
     * @return the specified parameter
     * @throws NullPointerException if {@code object} is {@code null}
     */
    public static <T> T requireNonNull(final T object, final String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }
        return object;
    }
}
