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

import java.util.List;
import java.util.Objects;
import javax.management.ObjectName;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * Implementation of the {@code LoggerConfigAdminMBean} interface.
 */
public class LoggerConfigAdmin implements LoggerConfigAdminMBean {

    private final LoggerContext loggerContext;
    private final LoggerConfig loggerConfig;
    private final ObjectName objectName;

    /**
     * Constructs a new {@code LoggerConfigAdmin} with the specified LoggerContext
     * and logger config.
     *
     * @param loggerContext used in the {@code ObjectName} for this mbean
     * @param loggerConfig the instrumented object
     */
    public LoggerConfigAdmin(final LoggerContext loggerContext, final LoggerConfig loggerConfig) {
        // super(executor); // no notifications for now
        this.loggerContext = Objects.requireNonNull(loggerContext, "loggerContext");
        this.loggerConfig = Objects.requireNonNull(loggerConfig, "loggerConfig");
        try {
            final String ctxName = Server.escape(loggerContext.getName());
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
        loggerContext.updateLoggers();
    }

    @Override
    public boolean isAdditive() {
        return loggerConfig.isAdditive();
    }

    @Override
    public void setAdditive(final boolean additive) {
        loggerConfig.setAdditive(additive);
        loggerContext.updateLoggers();
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
