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
package org.apache.logging.log4j.core.test.junit;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.plugins.util.Builder;

public class LoggingTestConfiguration implements Builder<LoggingTestContext> {
    private long timeout;
    private TimeUnit unit = TimeUnit.SECONDS;
    private String configurationLocation;
    private URI configUri;
    private String contextName;
    private ReconfigurationPolicy reconfigurationPolicy = ReconfigurationPolicy.NEVER;
    private boolean v1Config;
    private boolean bootstrap;
    private ClassLoader classLoader;

    public long getTimeout() {
        return timeout;
    }

    public LoggingTestConfiguration setTimeout(final long timeout, final TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
        return this;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public String getConfigurationLocation() {
        return configurationLocation;
    }

    public LoggingTestConfiguration setConfigurationLocation(final String configurationLocation) {
        this.configurationLocation = configurationLocation;
        return this;
    }

    public URI getConfigUri() {
        return configUri;
    }

    public LoggingTestConfiguration setConfigUri(final URI configUri) {
        this.configUri = configUri;
        return this;
    }

    public String getContextName() {
        return contextName;
    }

    public LoggingTestConfiguration setContextName(final String contextName) {
        this.contextName = contextName;
        return this;
    }

    public ReconfigurationPolicy getReconfigurationPolicy() {
        return reconfigurationPolicy;
    }

    public LoggingTestConfiguration setReconfigurationPolicy(final ReconfigurationPolicy reconfigurationPolicy) {
        this.reconfigurationPolicy = reconfigurationPolicy;
        return this;
    }

    public boolean isV1Config() {
        return v1Config;
    }

    public LoggingTestConfiguration setV1Config(final boolean v1Config) {
        this.v1Config = v1Config;
        return this;
    }

    public boolean isBootstrap() {
        return bootstrap;
    }

    public LoggingTestConfiguration setBootstrap(final boolean bootstrap) {
        this.bootstrap = bootstrap;
        return this;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public LoggingTestConfiguration setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @Override
    public LoggingTestContext build() {
        return new LoggingTestContext(this);
    }
}
