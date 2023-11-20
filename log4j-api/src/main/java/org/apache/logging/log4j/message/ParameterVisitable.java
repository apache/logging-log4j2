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

/**
 * Allows message parameters to be iterated over without any allocation
 * or memory copies.
 *
 * @since 2.11
 */
@PerformanceSensitive("allocation")
public interface ParameterVisitable {

    /**
     * Performs the given action for each parameter until all values
     * have been processed or the action throws an exception.
     * <p>
     * The second parameter lets callers pass in a stateful object to be modified with the key-value pairs,
     * so the TriConsumer implementation itself can be stateless and potentially reusable.
     * </p>
     *
     * @param action The action to be performed for each key-value pair in this collection
     * @param state the object to be passed as the third parameter to each invocation on the
     *          specified ParameterConsumer.
     * @param <S> type of the third parameter
     * @since 2.11
     */
    <S> void forEachParameter(ParameterConsumer<S> action, S state);
}
