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
package org.apache.logging.log4j.core.appender.rolling.action;

/**
 * Interface for objects that can be scheduled with a delay.
 * This provides a generic way to define delayed execution for any type of object,
 * not just actions. The delay time is specified in seconds.
 */
public interface Schedulable {

    /**
     * Gets the delay time in seconds before this object should be executed or processed.
     * A return value of 0 or negative means the object should be executed immediately.
     *
     * @return the delay in seconds (0 means execute immediately)
     */
    int getDelaySeconds();

    /**
     * Checks if this object should be delayed.
     *
     * @return true if the object has a positive delay
     */
    default boolean isDelayed() {
        return getDelaySeconds() > 0;
    }
}

