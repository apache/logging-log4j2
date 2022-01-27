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
package org.apache.logging.log4j.appserver.tomcat;

import org.apache.juli.WebappProperties;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

/**
 * Resolves the names specific to Tomcat's internal component structure. The
 * names of the properties starting with {@code classloader.} are kept for
 * compatibility with the original Tomcat JULI implementation.
 */
@Plugin(name = "tomcat", category = StrLookup.CATEGORY)
public class TomcatLookup implements StrLookup {

    private static final String SERVICE_LOGGER_FORMAT = "org.apache.catalina.core.ContainerBase.[%s]";
    private static final String HOST_LOGGER_FORMAT = "org.apache.catalina.core.ContainerBase.[%s].[%s]";
    private static final String CONTEXT_LOGGER_FORMAT = "org.apache.catalina.core.ContainerBase.[%s].[%s].[%s]";

    @Override
    public String lookup(String key) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl instanceof WebappProperties && key != null) {
            final WebappProperties props = (WebappProperties) cl;
            switch (key) {
                case "catalina.engine.name":
                case "classloader.serviceName":
                    return props.getServiceName();
                case "catalina.engine.logger":
                    return String.format(SERVICE_LOGGER_FORMAT, props.getServiceName());
                case "catalina.host.name":
                case "classloader.hostName":
                    return props.getHostName();
                case "catalina.host.logger":
                    return String.format(HOST_LOGGER_FORMAT, props.getServiceName(), props.getHostName());
                case "catalina.context.name":
                case "classloader.webappName":
                    return props.getWebappName();
                case "catalina.context.logger":
                    return String.format(CONTEXT_LOGGER_FORMAT, props.getServiceName(), props.getHostName(),
                            props.getWebappName());
            }
        }
        return null;
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }
}
