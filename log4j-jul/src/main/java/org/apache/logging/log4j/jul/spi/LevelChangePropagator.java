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
package org.apache.logging.log4j.jul.spi;

import org.apache.logging.log4j.jul.Log4jBridgeHandler;

/**
 * Propagates level configuration from the Log4j API implementation used
 * <p>
 *     Using the {@link Log4jBridgeHandler} is expensive, since disabled log events must be formatted by JUL,
 *     before they can be dropped by the Log4j API implementation.
 * </p>
 * <p>
 *     This class introduces a mechanism that can be implemented by each Log4j API implementation to be notified,
 *     whenever a {@link Log4jBridgeHandler} is used. The logging implementation will be able to synchronize
 *     its levels with the levels of JUL loggers each time it is reconfigured.
 * </p>
 * @since 3.0.0
 */
public interface LevelChangePropagator {

    /**
     * Start propagating log levels.
     */
    void start();

    /**
     * Stop propagating log levels.
     */
    void stop();
}
