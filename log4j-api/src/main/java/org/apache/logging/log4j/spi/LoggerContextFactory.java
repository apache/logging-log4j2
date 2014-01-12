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

import java.net.URI;

/**
 * Implemented by factories that create {@link LoggerContext} objects.
 */
public interface LoggerContextFactory {

    /**
     * Creates a {@link LoggerContext}.
     *
     * @param fqcn The fully qualified class name of the caller.
     * @param loader The ClassLoader to use or null.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @return The LoggerContext.
     */
    LoggerContext getContext(String fqcn, ClassLoader loader, Object externalContext, boolean currentContext);

    /**
     * Creates a {@link LoggerContext}.
     *
     * @param fqcn The fully qualified class name of the caller.
     * @param loader The ClassLoader to use or null.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @param configLocation The location of the configuration for the LoggerContext.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @param name The name of the context or null.
     * @return The LoggerContext.
     */
    LoggerContext getContext(String fqcn, ClassLoader loader, Object externalContext, boolean currentContext,
                             URI configLocation, String name);

    /**
     * Removes knowledge of a LoggerContext.
     *
     * @param context The context to remove.
     */
    void removeContext(LoggerContext context);
}
