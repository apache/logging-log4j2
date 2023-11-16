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
package org.apache.logging.log4j.core.appender;

import java.io.Serializable;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;

/**
 * Sends log events over HTTP.
 */
@Plugin(name = "Http", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class HttpAppender extends AbstractAppender {

    /**
     * Builds HttpAppender instances.
     * @param <B> The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<HttpAppender> {

        @PluginBuilderAttribute
        @Required(message = "No URL provided for HttpAppender")
        private URL url;

        @PluginBuilderAttribute
        private String method = "POST";

        @PluginBuilderAttribute
        private int connectTimeoutMillis = 0;

        @PluginBuilderAttribute
        private int readTimeoutMillis = 0;

        @PluginElement("Headers")
        private Property[] headers;

        @PluginElement("SslConfiguration")
        private SslConfiguration sslConfiguration;

        @PluginBuilderAttribute
        private boolean verifyHostname = true;

        @Override
        public HttpAppender build() {
            final HttpManager httpManager = new HttpURLConnectionManager(
                    getConfiguration(),
                    getConfiguration().getLoggerContext(),
                    getName(),
                    url,
                    method,
                    connectTimeoutMillis,
                    readTimeoutMillis,
                    headers,
                    sslConfiguration,
                    verifyHostname);
            return new HttpAppender(
                    getName(), getLayout(), getFilter(), isIgnoreExceptions(), httpManager, getPropertyArray());
        }

        public URL getUrl() {
            return url;
        }

        public String getMethod() {
            return method;
        }

        public int getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public int getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public Property[] getHeaders() {
            return headers;
        }

        public SslConfiguration getSslConfiguration() {
            return sslConfiguration;
        }

        public boolean isVerifyHostname() {
            return verifyHostname;
        }

        public B setUrl(final URL url) {
            this.url = url;
            return asBuilder();
        }

        public B setMethod(final String method) {
            this.method = method;
            return asBuilder();
        }

        public B setConnectTimeoutMillis(final int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return asBuilder();
        }

        public B setReadTimeoutMillis(final int readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
            return asBuilder();
        }

        public B setHeaders(final Property[] headers) {
            this.headers = headers;
            return asBuilder();
        }

        public B setSslConfiguration(final SslConfiguration sslConfiguration) {
            this.sslConfiguration = sslConfiguration;
            return asBuilder();
        }

        public B setVerifyHostname(final boolean verifyHostname) {
            this.verifyHostname = verifyHostname;
            return asBuilder();
        }
    }

    /**
     * @return a builder for a HttpAppender.
     */
    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private final HttpManager manager;

    private HttpAppender(
            final String name,
            final Layout<? extends Serializable> layout,
            final Filter filter,
            final boolean ignoreExceptions,
            final HttpManager manager,
            final Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        Objects.requireNonNull(layout, "layout");
        this.manager = Objects.requireNonNull(manager, "manager");
    }

    @Override
    public void start() {
        super.start();
        manager.startup();
    }

    @Override
    public void append(final LogEvent event) {
        try {
            manager.send(getLayout(), event);
        } catch (final Exception e) {
            error("Unable to send HTTP in appender [" + getName() + "]", event, e);
        }
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        boolean stopped = super.stop(timeout, timeUnit, false);
        stopped &= manager.stop(timeout, timeUnit);
        setStopped();
        return stopped;
    }

    @Override
    public String toString() {
        return "HttpAppender{" + "name=" + getName() + ", state=" + getState() + '}';
    }
}
