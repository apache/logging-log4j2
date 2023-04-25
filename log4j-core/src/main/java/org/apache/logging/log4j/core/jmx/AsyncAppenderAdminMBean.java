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
package org.apache.logging.log4j.core.jmx;

/**
 * The MBean interface for monitoring and managing an {@code AsyncAppender}.
 */
public interface AsyncAppenderAdminMBean {
    /**
     * ObjectName pattern ({@value} ) for AsyncAppenderAdmin MBeans. This
     * pattern contains two variables, where the first is the name of the
     * context, the second is the name of the instrumented appender.
     * <p>
     * You can find all registered AsyncAppenderAdmin MBeans like this:
     * </p>
     *
     * <pre>
     * MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
     * String pattern = String.format(AsyncAppenderAdminMBean.PATTERN, &quot;*&quot;, &quot;*&quot;);
     * Set&lt;ObjectName&gt; appenderNames = mbs.queryNames(new ObjectName(pattern), null);
     * </pre>
     * <p>
     * Some characters are not allowed in ObjectNames. The logger context name
     * and appender name may be quoted. When AsyncAppenderAdmin MBeans are
     * registered, their ObjectNames are created using this pattern as follows:
     * </p>
     *
     * <pre>
     * String ctxName = Server.escape(loggerContext.getName());
     * String appenderName = Server.escape(appender.getName());
     * String name = String.format(PATTERN, ctxName, appenderName);
     * ObjectName objectName = new ObjectName(name);
     * </pre>
     *
     * @see Server#escape(String)
     */
    String PATTERN = Server.DOMAIN + ":type=%s,component=AsyncAppenders,name=%s";

    /**
     * Returns the name of the instrumented {@code AsyncAppender}.
     *
     * @return the name of the AsyncAppender
     */
    String getName();

    /**
     * Returns the result of calling {@code toString} on the {@code Layout}
     * object of the instrumented {@code AsyncAppender}.
     *
     * @return the {@code Layout} of the instrumented {@code AsyncAppender} as a
     *         string
     */
    String getLayout();

    /**
     * Returns how exceptions thrown on the instrumented {@code AsyncAppender}
     * are handled.
     *
     * @return {@code true} if any exceptions thrown by the AsyncAppender will
     *         be logged or {@code false} if such exceptions are re-thrown.
     */
    boolean isIgnoreExceptions();

    /**
     * Returns the result of calling {@code toString} on the error handler of
     * this appender, or {@code "null"} if no error handler was set.
     *
     * @return result of calling {@code toString} on the error handler of this
     *         appender, or {@code "null"}
     */
    String getErrorHandler();

    /**
     * Returns a string description of all filters configured for the
     * instrumented {@code AsyncAppender}.
     *
     * @return a string description of all configured filters for this appender
     */
    String getFilter();

    /**
     * Returns a String array with the appender refs configured for the
     * instrumented {@code AsyncAppender}.
     *
     * @return the appender refs for the instrumented {@code AsyncAppender}.
     */
    String[] getAppenderRefs();

    /**
     * Returns {@code true} if this AsyncAppender will take a snapshot of the
     * stack with every log event to determine the class and method where the
     * logging call was made.
     *
     * @return {@code true} if location is included with every event,
     *         {@code false} otherwise
     */
    boolean isIncludeLocation();

    /**
     * Returns {@code true} if this AsyncAppender will block when the queue is
     * full, or {@code false} if events are dropped when the queue is full.
     *
     * @return whether this AsyncAppender will block or drop events when the
     *         queue is full.
     */
    boolean isBlocking();

    /**
     * Returns the name of the appender that any errors are logged to or {@code null}.
     * @return the name of the appender that any errors are logged to or {@code null}
     */
    String getErrorRef();

    int getQueueCapacity();

    int getQueueRemainingCapacity();
}
