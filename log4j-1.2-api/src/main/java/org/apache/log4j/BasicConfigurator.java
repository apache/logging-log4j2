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
package org.apache.log4j;

import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * Configures the package.
 *
 * <p>
 * For file based configuration, see {@link PropertyConfigurator}. For XML based configuration, see
 * {@link org.apache.log4j.xml.DOMConfigurator DOMConfigurator}.
 * </p>
 *
 * @since 0.8.1
 */
public class BasicConfigurator {

    /**
     * Adds a {@link ConsoleAppender} that uses {@link PatternLayout} using the
     * {@link PatternLayout#TTCC_CONVERSION_PATTERN} and prints to <code>System.out</code> to the root category.
     */
    public static void configure() {
        LogManager.reconfigure(StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Adds <code>appender</code> to the root category.
     *
     * @param appender The appender to add to the root category.
     */
    public static void configure(final Appender appender) {
        LogManager.getRootLogger(StackLocatorUtil.getCallerClassLoader(2)).addAppender(appender);
    }

    /**
     * Resets the default hierarchy to its default. It is equivalent to calling
     * <code>Category.getDefaultHierarchy().resetConfiguration()</code>.
     *
     * See {@link Hierarchy#resetConfiguration()} for more details.
     */
    public static void resetConfiguration() {
        LogManager.resetConfiguration(StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Constructs a new instance.
     */
    protected BasicConfigurator() {}
}
