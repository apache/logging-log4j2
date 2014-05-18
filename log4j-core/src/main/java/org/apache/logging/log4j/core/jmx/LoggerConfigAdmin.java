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

import java.util.List;

import javax.management.ObjectName;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.util.Assert;

/**
 * Implementation of the {@code LoggerConfigAdminMBean} interface.
 */
public class LoggerConfigAdmin implements LoggerConfigAdminMBean {

    private final String contextName;
    private final LoggerConfig loggerConfig;
    private final ObjectName objectName;

    /**
     * Constructs a new {@code LoggerConfigAdmin} with the specified contextName
     * and logger config.
     *
     * @param contextName used in the {@code ObjectName} for this mbean
     * @param loggerConfig the instrumented object
     */
    public LoggerConfigAdmin(final String contextName, final LoggerConfig loggerConfig) {
        // super(executor); // no notifications for now
        this.contextName = Assert.requireNonNull(contextName, "contextName");
        this.loggerConfig = Assert.requireNonNull(loggerConfig, "loggerConfig");
        try {
            final String ctxName = Server.escape(this.contextName);
            final String configName = Server.escape(loggerConfig.getName());
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
     * @see LoggerConfigAdminMBean#PATTERN
     */
    public ObjectName getObjectName() {
        return objectName;
    }

    @Override
    public String getName() {
        return loggerConfig.getName();
    }

    @Override
    public String getLevel() {
        return loggerConfig.getLevel().name();
    }

    @Override
    public void setLevel(final String level) {
        loggerConfig.setLevel(Level.getLevel(level));
    }

    @Override
    public boolean isAdditive() {
        return loggerConfig.isAdditive();
    }

    @Override
    public void setAdditive(final boolean additive) {
        loggerConfig.setAdditive(additive);
    }

    @Override
    public boolean isIncludeLocation() {
        return loggerConfig.isIncludeLocation();
    }

    @Override
    public String getFilter() {
        return String.valueOf(loggerConfig.getFilter());
    }

    @Override
    public String[] getAppenderRefs() {
        final List<AppenderRef> refs = loggerConfig.getAppenderRefs();
        final String[] result = new String[refs.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = refs.get(i).getRef();
        }
        return result;
    }
}
