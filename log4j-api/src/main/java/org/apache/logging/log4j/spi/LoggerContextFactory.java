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
package org.apache.logging.log4j.spi;

import java.net.URI;

/**
 * Implemented by factories that create {@link LoggerContext} objects.
 */
public interface LoggerContextFactory {

    /**
     * Shuts down the LoggerContext.
     * @param fqcn The fully qualified class name of the caller.
     * @param loader The ClassLoader to use or null.
     * @param currentContext If true shuts down the current Context, if false shuts down the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @param allContexts if true all LoggerContexts that can be located will be shutdown.
     * @since 2.13.0
     */
    default void shutdown(String fqcn, ClassLoader loader, boolean currentContext, boolean allContexts) {
        if (hasContext(fqcn, loader, currentContext)) {
            final LoggerContext ctx = getContext(fqcn, loader, null, currentContext);
            if (ctx instanceof Terminable) {
                ((Terminable) ctx).terminate();
            }
        }
    }

    /**
     * Checks to see if a LoggerContext is installed. The default implementation returns false.
     * @param fqcn The fully qualified class name of the caller.
     * @param loader The ClassLoader to use or null.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @return true if a LoggerContext has been installed, false otherwise.
     * @since 2.13.0
     */
    default boolean hasContext(String fqcn, ClassLoader loader, boolean currentContext) {
        return false;
    }

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
    LoggerContext getContext(
            String fqcn,
            ClassLoader loader,
            Object externalContext,
            boolean currentContext,
            URI configLocation,
            String name);

    /**
     * Removes knowledge of a LoggerContext.
     *
     * @param context The context to remove.
     */
    void removeContext(LoggerContext context);

    /**
     * Determines whether or not this factory and perhaps the underlying
     * ContextSelector behavior depend on the callers classloader.
     *
     * This method should be overridden by implementations, however a default method is provided which always
     * returns {@code true} to preserve the old behavior.
     *
     * @since 2.15.0
     */
    default boolean isClassLoaderDependent() {
        return true;
    }
}
