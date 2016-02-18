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

import org.apache.logging.log4j.message.Message;


/**
 * Utility class for lambda support.
 */
public final class LambdaUtil {
    /**
     * Private constructor: this class is not intended to be instantiated.
     */
    private LambdaUtil() {
    }

    /**
     * Converts an array of lambda expressions into an array of their evaluation results.
     *
     * @param suppliers an array of lambda expressions or {@code null}
     * @return an array containing the results of evaluating the lambda expressions (or {@code null} if the suppliers
     *         array was {@code null}
     */
    public static Object[] getAll(final Supplier<?>... suppliers) {
        if (suppliers == null) {
            return null;
        }
        final Object[] result = new Object[suppliers.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = get(suppliers[i]);
        }
        return result;
    }

    /**
     * Returns the result of evaluating the specified function.
     * @param supplier a lambda expression or {@code null}
     * @return the results of evaluating the lambda expression (or {@code null} if the supplier
     *         was {@code null}
     */
    public static Object get(final Supplier<?> supplier) {
        if (supplier == null) {
            return null;
        }
        Object result = supplier.get();
        return result instanceof Message ? ((Message) result).getFormattedMessage() : result;
    }

    /**
     * Returns the Message supplied by the specified function.
     * @param supplier a lambda expression or {@code null}
     * @return the Message resulting from evaluating the lambda expression (or {@code null} if the supplier was
     * {@code null}
     */
    public static Message get(final MessageSupplier supplier) {
        if (supplier == null) {
            return null;
        }
        return supplier.get();
    }
}
