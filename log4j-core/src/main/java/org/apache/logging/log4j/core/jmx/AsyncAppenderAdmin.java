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

import java.util.Objects;
import javax.management.ObjectName;
import org.apache.logging.log4j.core.appender.AsyncAppender;

/**
 * Implementation of the {@code AsyncAppenderAdminMBean} interface.
 */
public class AsyncAppenderAdmin implements AsyncAppenderAdminMBean {

    private final String contextName;
    private final AsyncAppender asyncAppender;
    private final ObjectName objectName;

    /**
     * Constructs a new {@code AsyncAppenderAdmin} with the specified contextName
     * and async appender.
     *
     * @param contextName used in the {@code ObjectName} for this mbean
     * @param appender the instrumented object
     */
    public AsyncAppenderAdmin(final String contextName, final AsyncAppender appender) {
        // super(executor); // no notifications for now
        this.contextName = Objects.requireNonNull(contextName, "contextName");
        this.asyncAppender = Objects.requireNonNull(appender, "async appender");
        try {
            final String ctxName = Server.escape(this.contextName);
            final String configName = Server.escape(appender.getName());
            final String name = String.format(PATTERN, ctxName, configName);
            objectName = new ObjectName(name);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the {@code ObjectName} of this mbean.
     *
     * @return the {@code ObjectName}
     * @see AppenderAdminMBean#PATTERN
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public String getName() {
        return asyncAppender.getName();
    }

    @Override
    public String getLayout() {
        return String.valueOf(asyncAppender.getLayout());
    }

    @Override
    public boolean isIgnoreExceptions() {
        return asyncAppender.ignoreExceptions();
    }

    @Override
    public String getErrorHandler() {
        return String.valueOf(asyncAppender.getHandler());
    }

    @Override
    public String getFilter() {
        return String.valueOf(asyncAppender.getFilter());
    }

    @Override
    public String[] getAppenderRefs() {
        return asyncAppender.getAppenderRefStrings();
    }

    /**
     * Returns {@code true} if this AsyncAppender will take a snapshot of the stack with
     * every log event to determine the class and method where the logging call
     * was made.
     * @return {@code true} if location is included with every event, {@code false} otherwise
     */
    @Override
    public boolean isIncludeLocation() {
        return asyncAppender.isIncludeLocation();
    }

    /**
     * Returns {@code true} if this AsyncAppender will block when the queue is full,
     * or {@code false} if events are dropped when the queue is full.
     * @return whether this AsyncAppender will block or drop events when the queue is full.
     */
    @Override
    public boolean isBlocking() {
        return asyncAppender.isBlocking();
    }

    /**
     * Returns the name of the appender that any errors are logged to or {@code null}.
     * @return the name of the appender that any errors are logged to or {@code null}
     */
    @Override
    public String getErrorRef() {
        return asyncAppender.getErrorRef();
    }

    @Override
    public int getQueueCapacity() {
        return asyncAppender.getQueueCapacity();
    }

    @Override
    public int getQueueRemainingCapacity() {
        return asyncAppender.getQueueRemainingCapacity();
    }
}
