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
package org.apache.logging.log4j.message;

import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * Messages implementing this interface are reused between logging calls.
 * <p>
 * If a Message is reusable, downstream components should not hand over this instance to another thread, but extract its
 * content (via the {@link StringBuilderFormattable#formatTo(StringBuilder)} method) instead.
 * </p>
 * @see ReusableMessageFactory
 * @since 2.6
 */
@PerformanceSensitive("allocation")
public interface ReusableMessage extends Message, StringBuilderFormattable {

    /**
     * Returns the parameter array that was used to initialize this reusable message and replaces it with the specified
     * array. The returned parameter array will no longer be modified by this reusable message. The specified array is
     * now "owned" by this reusable message and can be modified if necessary for the next log event.
     * </p><p>
     * ReusableMessages that have no parameters return the specified array.
     * </p><p>
     * This method is used by asynchronous loggers to pass the parameter array to a background thread without
     * allocating new objects.
     * The actual number of parameters in the returned array can be determined with {@link #getParameterCount()}.
     * </p>
     *
     * @param emptyReplacement the parameter array that can be used for subsequent uses of this reusable message.
     *         This replacement array must have at least 10 elements (the number of varargs supported by the Logger
     *         API).
     * @return the parameter array for the current message content. This may be a vararg array of any length, or it may
     *         be a reusable array of 10 elements used to hold the unrolled vararg parameters.
     * @see #getParameterCount()
     */
    Object[] swapParameters(Object[] emptyReplacement);

    /**
     * Returns the number of parameters that was used to initialize this reusable message for the current content.
     * <p>
     * The parameter array returned by {@link #swapParameters(Object[])} may be larger than the actual number of
     * parameters. Callers should use this method to determine how many elements the array contains.
     * </p>
     * @return the current number of parameters
     */
    short getParameterCount();

    /**
     * Returns an immutable snapshot of the current internal state of this reusable message. The returned snapshot
     * will not be affected by subsequent modifications of this reusable message.
     *
     * @return an immutable snapshot of this message
     */
    Message memento();
}
