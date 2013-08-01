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
package org.apache.logging.log4j.core.helpers;

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
     * Usage:
     *
     * <pre>
     * // earlier you would write this:
     * public SomeConstructor(Object param) {
     *     if (param == null) {
     *         throw new NullPointerException(name + &quot; is null&quot;);
     *     }
     *     this.field = param;
     * }
     *
     * // now you can do the same in one line:
     * public SomeConstructor(Object param) {
     *     this.field = Assert.isNotNull(param);
     * }
     * </pre>
     *
     * @param <T> the type of the parameter to check and return
     * @param checkMe the parameter to check
     * @param name name of the parameter to use in the error message if
     *            {@code null}
     * @return the specified parameter
     */
    public static <T> T isNotNull(final T checkMe, final String name) {
        if (checkMe == null) {
            throw new NullPointerException(name + " is null");
        }
        return checkMe;
    }
}
