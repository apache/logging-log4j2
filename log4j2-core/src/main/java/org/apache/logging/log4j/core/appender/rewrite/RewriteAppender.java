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
package org.apache.logging.log4j.core.appender.rewrite;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderBase;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This Appender allows the logging event to be manipulated before it is processed by other Appenders.
 */
@Plugin(name="Rewrite",type="Core",elementType="appender",printObject=true)
public class RewriteAppender extends AppenderBase {
    private final Configuration config;
    private ConcurrentMap<String, AppenderControl> appenders = new ConcurrentHashMap<String, AppenderControl>();
    private final RewritePolicy rewritePolicy;
    private final String[] appenderRefs;

    private RewriteAppender(String name, Filter filter, boolean handleException, String[] appenderRefs,
                            RewritePolicy rewritePolicy, Configuration config) {
        super(name, filter, null, handleException);
        this.config = config;
        this.rewritePolicy = rewritePolicy;
        this.appenderRefs = appenderRefs;
    }

    @Override
    public void start() {
        Map<String, Appender> map = config.getAppenders();
        for (String ref : appenderRefs) {
            Appender appender = map.get(ref);
            if (appender != null) {
                appenders.put(ref, new AppenderControl(appender));
            } else {
                LOGGER.error("Appender " + ref + " cannot be located. Reference ignored");
            }
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        for (AppenderControl control : appenders.values()) {
            if (control instanceof AppenderWrapper) {
                control.getAppender().stop();
            }
        }
    }

    public void append(LogEvent event) {
        if (rewritePolicy != null) {
            event = rewritePolicy.rewrite(event);
        }
        for (AppenderControl control : appenders.values()) {
            control.callAppender(event);
        }
    }

    @PluginFactory
    public static RewriteAppender createAppender(@PluginAttr("name") String name,
                                          @PluginAttr("suppressExceptions") String suppress,
                                          @PluginElement("appender-ref") String[] appenderRefs,
                                          @PluginConfiguration Configuration config,
                                          @PluginElement("rewritePolicy") RewritePolicy rewritePolicy,
                                          @PluginElement("filters") Filter filter) {

        boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);

        if (name == null) {
            LOGGER.error("No name provided for RewriteAppender");
            return null;
        }
        if (appenderRefs == null) {
            LOGGER.error("No appender references defined for RewriteAppender");
            return null;
        }
        return new RewriteAppender(name, filter, handleExceptions, appenderRefs, rewritePolicy, config);
    }

    private static class AppenderWrapper extends AppenderControl {
        public AppenderWrapper(Appender appender) {
            super(appender);
        }
    }
}
