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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Property;

public class HttpManager extends AbstractManager {

    private final Configuration configuration;
    private final URL url;
    private final String method;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final Property[] headers;

    public HttpManager(final Configuration configuration, LoggerContext loggerContext, final String name,
                       final String url, final String method, final int connectTimeoutMillis, final int readTimeoutMillis,
                       final Property[] headers) {
        super(loggerContext, name);
        this.configuration = Objects.requireNonNull(configuration);
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new ConfigurationException(e);
        }
        this.method = Objects.requireNonNull(method, "method");
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.headers = headers != null ? headers : new Property[0];
    }

    public void send(final Layout<?> layout, final LogEvent event) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        urlConnection.setAllowUserInteraction(false);
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setRequestMethod(method);
        if (connectTimeoutMillis > 0) urlConnection.setConnectTimeout(connectTimeoutMillis);
        if (readTimeoutMillis > 0) urlConnection.setReadTimeout(readTimeoutMillis);
        if (layout.getContentType() != null) urlConnection.setRequestProperty("Content-Type", layout.getContentType());
        for (Property header : headers) {
            urlConnection.setRequestProperty(
                header.getName(),
                header.isValueNeedsLookup() ? configuration.getStrSubstitutor().replace(event, header.getValue()) : header.getValue());
        }
        byte[] msg = layout.toByteArray(event);
        urlConnection.setFixedLengthStreamingMode(msg.length);
        urlConnection.connect();
        try (OutputStream os = urlConnection.getOutputStream()) {
            os.write(msg);
        }
        urlConnection.getInputStream().close();
    }

}
