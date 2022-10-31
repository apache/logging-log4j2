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

package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.PropertyEnvironment;

/**
 * Central service provider for the logging system used by Log4j API. Implementations provide factory instances
 * for loggers, thread context maps, and thread context stacks, along with configuration properties.
 *
 * @see AbstractLoggingSystemProvider
 * @see PropertyEnvironment
 * @see LoggerContextFactory
 * @see ThreadContextMap.Factory
 * @see ThreadContextStack.Factory
 */
public interface LoggingSystemProvider {

    /**
     * Returns the priority of this provider where larger numbers indicate higher priority.
     */
    int getPriority();

    /**
     * Returns the minimum valid API version this provider requires.
     */
    String getVersion();

    /**
     * Gets an instance of the specified class or interface. This should minimally support construction
     * of classes with a default constructor along with knowledge of bindings of existing instances to interfaces.
     * More sophisticated implementations may support dependency injection and configuration binding.
     */
    <T> T getInstance(final Class<T> clazz);

    /**
     * Returns the properties this logging system was configured with.
     */
    default PropertyEnvironment getEnvironment() {
        return getInstance(PropertyEnvironment.class);
    }

    /**
     * Returns the initialized LoggerContextFactory to use for managing {@link LoggerContext} and
     * {@link Logger} instances.
     */
    default LoggerContextFactory getLoggerContextFactory() {
        return getInstance(LoggerContextFactory.class);
    }

    /**
     * Returns the initialized {@link ThreadContextMap.Factory} to use for creating {@link ThreadContextMap} instances.
     */
    default ThreadContextMap.Factory getContextMapFactory() {
        return getInstance(ThreadContextMap.Factory.class);
    }

    /**
     * Returns the initialized {@link ThreadContextStack.Factory} to use for creating {@link ThreadContextStack}
     * instances.
     */
    default ThreadContextStack.Factory getContextStackFactory() {
        return getInstance(ThreadContextStack.Factory.class);
    }

}
