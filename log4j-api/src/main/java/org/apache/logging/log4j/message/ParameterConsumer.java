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

/**
 * An operation that accepts two input arguments and returns no result.
 *
 * <p>
 * The third parameter lets callers pass in a stateful object to be modified with the key-value pairs,
 * so the ParameterConsumer implementation itself can be stateless and potentially reusable.
 * </p>
 *
 * @param <S> state data
 * @see ReusableMessage
 * @since 2.11
 */
public interface ParameterConsumer<S> {

    /**
     * Performs an operation given the specified arguments.
     *
     * @param parameter the parameter
     * @param parameterIndex Index of the parameter
     * @param state the state data
     */
    void accept(Object parameter, int parameterIndex, S state);
}
