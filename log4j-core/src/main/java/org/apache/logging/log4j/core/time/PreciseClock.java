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
package org.apache.logging.log4j.core.time;

import org.apache.logging.log4j.core.util.Clock;

/**
 * Extension of the {@link Clock} interface that is able to provide more accurate time information than milliseconds
 * since the epoch. {@code PreciseClock} implementations are free to return millisecond-precision time
 * if that is the most accurate time information available on this platform.
 * @since 2.11
 */
public interface PreciseClock extends Clock {

    /**
     * Initializes the specified instant with time information as accurate as available on this platform.
     * @param mutableInstant the container to be initialized with the accurate time information
     * @since 2.11
     */
    void init(final MutableInstant mutableInstant);
}
