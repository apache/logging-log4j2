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
package org.apache.logging.log4j.web;

import javax.servlet.UnavailableException;

/**
 * Specifies an interface for initializing and deinitializing Log4j in a Java EE web application. The default and only
 * implementation is {@link Log4jWebInitializerImpl}. The initializer is based on an interface to improve testability.
 * The methods here are contained in a package-private sub-interface because general application code should not have
 * access to them.
 */
interface Log4jWebInitializer extends Log4jWebSupport {
    /**
     * Starts up Log4j in the web application. Calls {@link #setLoggerContext()} after initialization is complete.
     *
     * @throws UnavailableException if a JNDI config location is specified but no name is specified.
     */
    void initialize() throws UnavailableException;

    /**
     * Shuts down Log4j in the web application. Calls {@link #clearLoggerContext()} immediately before deinitialization
     * begins.
     */
    void deinitialize();
}
