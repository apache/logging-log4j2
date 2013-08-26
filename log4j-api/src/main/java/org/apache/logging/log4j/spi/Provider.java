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
package org.apache.logging.log4j.spi;

import java.net.URL;
import java.util.Properties;

/**
 *
 */
public class Provider {
    private static final Integer DEFAULT_PRIORITY = Integer.valueOf(-1);
    private static final String FACTORY_PRIORITY = "FactoryPriority";
    private static final String THREAD_CONTEXT_MAP = "ThreadContextMap";
    private static final String LOGGER_CONTEXT_FACTORY = "LoggerContextFactory";

    private final Integer priority;
    private final String className;
    private final String threadContextMap;
    private final URL url;

    public Provider(final Properties props, final URL url) {
        this.url = url;
        final String weight = props.getProperty(FACTORY_PRIORITY);
        priority = weight == null ? DEFAULT_PRIORITY : Integer.valueOf(weight);
        className = props.getProperty(LOGGER_CONTEXT_FACTORY);
        threadContextMap = props.getProperty(THREAD_CONTEXT_MAP);
    }

    public Integer getPriority() {
        return priority;
    }

    public String getClassName() {
        return className;
    }

    public String getThreadContextMap() {
        return threadContextMap;
    }

    public URL getURL() {
        return url;
    }
}
