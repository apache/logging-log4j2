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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.Filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The FailoverAppender will capture exceptions in an Appender and then route the event
 * to a different appender. Hopefully it is obvious that the Appenders must be configured
 * to not suppress exceptions for the FailoverAppender to work.
 */
@Plugin(name="Failover",type="Core",elementType="appender",printObject=true)
public class FailoverAppender extends AppenderBase {

    private final String primaryRef;

    private final String[] failovers;

    private final Configuration config;

    private AppenderControl primary;

    private List<AppenderControl> failoverAppenders = new ArrayList<AppenderControl>();


    public FailoverAppender(String name, Filters filters, String primary, String[] failovers,
                            Configuration config, boolean handleExceptions) {
        super(name, filters, null, handleExceptions);
        this.primaryRef = primary;
        this.failovers = failovers;
        this.config = config;
    }


    @Override
    public void start() {
        Map<String, Appender> map = config.getAppenders();
        int errors = 0;
        if (map.containsKey(primaryRef)) {
            primary = new AppenderControl(map.get(primaryRef));
        } else {
            logger.error("Unable to locate primary Appender " + primaryRef);
            ++errors;
        }
        for (String name : failovers) {
            if (map.containsKey(name)) {
                failoverAppenders.add(new AppenderControl(map.get(name)));
            } else {
                logger.error("Failover appender " + name + " is not configured");
            }
        }
        if (failoverAppenders.size() == 0) {
            logger.error("No failover appenders are available");
            ++errors;
        }
        if (errors == 0) {
            super.start();
        }
    }

    @Override
    public void stop() {
        super.stop();
    }

    public void append(LogEvent event) {
        RuntimeException re = null;
        if (!isStarted()) {
            error("FailoverAppender " + getName() + " did not start successfully");
            return;
        }
        try {
            primary.callAppender(event);
        } catch (Exception ex) {
            re = new LoggingException(ex);
            boolean written = false;
            for (AppenderControl control : failoverAppenders) {
                try {
                    control.callAppender(event);
                    written = true;
                    break;
                } catch (Exception fex) {
                    // Try the next failover.
                }
            }
            if (!written && !isExceptionSuppressed()) {
                throw re;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());
        sb.append(" primary=").append(primary).append(", failover={");
        boolean first = true;
        for (String str : failovers) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(str);
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @PluginFactory
    public static FailoverAppender createAppender(@PluginAttr("name") String name,
                                                  @PluginAttr("primary") String primary,
                                                  @PluginElement("failovers") String[] failovers,
                                                  @PluginConfiguration Configuration config,
                                                  @PluginElement("filters") Filters filters,
                                                  @PluginAttr("suppressExceptions") String suppress) {
        if (name == null) {
            logger.error("A name for the Appender must be specified");
            return null;
        }
        if (primary == null) {
            logger.error("A primary Appender must be specified");
            return null;
        }
        if (failovers == null || failovers.length == 0) {
            logger.error("At least one failover Appender must be specified");
            return null;
        }

        boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);

        return new FailoverAppender(name, filters, primary, failovers, config, handleExceptions);
    }
}
