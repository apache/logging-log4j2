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
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.filter.AbstractFilterable;

/**
 * Implementation of the {@code AppenderAdminMBean} interface.
 */
public class AppenderAdmin implements AppenderAdminMBean {

    private final String contextName;
    private final Appender appender;
    private final ObjectName objectName;

    /**
     * Constructs a new {@code AppenderAdmin} with the specified contextName
     * and appender.
     *
     * @param contextName used in the {@code ObjectName} for this mbean
     * @param appender the instrumented object
     */
    public AppenderAdmin(final String contextName, final Appender appender) {
        // super(executor); // no notifications for now
        this.contextName = Objects.requireNonNull(contextName, "contextName");
        this.appender = Objects.requireNonNull(appender, "appender");
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
        return appender.getName();
    }

    @Override
    public String getLayout() {
        return String.valueOf(appender.getLayout());
    }

    @Override
    public boolean isIgnoreExceptions() {
        return appender.ignoreExceptions();
    }

    @Override
    public String getErrorHandler() {
        return String.valueOf(appender.getHandler());
    }

    @Override
    public String getFilter() {
        if (appender instanceof AbstractFilterable) {
            return String.valueOf(((AbstractFilterable) appender).getFilter());
        }
        return null;
    }
}
