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
package org.apache.logging.log4j.core.jmx;

/**
 * The MBean interface for monitoring and managing an {@code Appender}.
 */
public interface AppenderAdminMBean {
    /** ObjectName pattern for AppenderAdmin MBeans. */
    String PATTERN = "org.apache.logging.log4j2:type=LoggerContext,ctx=%s,sub=Appender,name=%s";

    /**
     * Returns the name of the instrumented {@code Appender}.
     * 
     * @return the name of the Appender
     */
    String getName();

    /**
     * Returns the result of calling {@code toString} on the {@code Layout}
     * object of the instrumented {@code Appender}.
     * 
     * @return the {@code Layout} of the instrumented {@code Appender} as a
     *         string
     */
    String getLayout();

    /**
     * Returns how exceptions thrown on the instrumented {@code Appender} are
     * handled.
     * 
     * @return {@code true} if any exceptions thrown by the Appender will be
     *         logged or {@code false} if such exceptions are re-thrown.
     */
    boolean isExceptionSuppressed();

    /**
     * Returns the result of calling {@code toString} on the error handler of
     * this appender, or {@code "null"} if no error handler was set.
     * 
     * @return result of calling {@code toString} on the error handler of this
     *         appender, or {@code "null"}
     */
    String getErrorHandler();
}
