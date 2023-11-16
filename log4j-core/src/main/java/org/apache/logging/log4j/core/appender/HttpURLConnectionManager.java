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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.net.ssl.LaxHostnameVerifier;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.IOUtils;

public class HttpURLConnectionManager extends HttpManager {

    private static final Charset CHARSET = Charset.forName("US-ASCII");

    private final URL url;
    private final boolean isHttps;
    private final String method;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final Property[] headers;
    private final SslConfiguration sslConfiguration;
    private final boolean verifyHostname;

    public HttpURLConnectionManager(
            final Configuration configuration,
            final LoggerContext loggerContext,
            final String name,
            final URL url,
            final String method,
            final int connectTimeoutMillis,
            final int readTimeoutMillis,
            final Property[] headers,
            final SslConfiguration sslConfiguration,
            final boolean verifyHostname) {
        super(configuration, loggerContext, name);
        this.url = url;
        if (!(url.getProtocol().equalsIgnoreCase("http") || url.getProtocol().equalsIgnoreCase("https"))) {
            throw new ConfigurationException("URL must have scheme http or https");
        }
        this.isHttps = this.url.getProtocol().equalsIgnoreCase("https");
        this.method = Objects.requireNonNull(method, "method");
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.headers = headers != null ? headers : Property.EMPTY_ARRAY;
        this.sslConfiguration = sslConfiguration;
        if (this.sslConfiguration != null && !isHttps) {
            throw new ConfigurationException("SSL configuration can only be specified with URL scheme https");
        }
        this.verifyHostname = verifyHostname;
    }

    @Override
    @SuppressFBWarnings(
            value = "URLCONNECTION_SSRF_FD",
            justification = "This connection URL is specified in a configuration file.")
    public void send(final Layout<?> layout, final LogEvent event) throws IOException {
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setAllowUserInteraction(false);
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setRequestMethod(method);
        if (connectTimeoutMillis > 0) {
            urlConnection.setConnectTimeout(connectTimeoutMillis);
        }
        if (readTimeoutMillis > 0) {
            urlConnection.setReadTimeout(readTimeoutMillis);
        }
        if (layout.getContentType() != null) {
            urlConnection.setRequestProperty("Content-Type", layout.getContentType());
        }
        for (final Property header : headers) {
            urlConnection.setRequestProperty(
                    header.getName(), header.evaluate(getConfiguration().getStrSubstitutor()));
        }
        if (sslConfiguration != null) {
            ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslConfiguration.getSslSocketFactory());
        }
        if (isHttps && !verifyHostname) {
            ((HttpsURLConnection) urlConnection).setHostnameVerifier(LaxHostnameVerifier.INSTANCE);
        }

        final byte[] msg = layout.toByteArray(event);
        urlConnection.setFixedLengthStreamingMode(msg.length);
        urlConnection.connect();
        try (final OutputStream os = urlConnection.getOutputStream()) {
            os.write(msg);
        }

        final byte[] buffer = new byte[1024];
        try (final InputStream is = urlConnection.getInputStream()) {
            while (IOUtils.EOF != is.read(buffer)) {
                // empty
            }
        } catch (final IOException e) {
            final StringBuilder errorMessage = new StringBuilder();
            try (final InputStream es = urlConnection.getErrorStream()) {
                errorMessage.append(urlConnection.getResponseCode());
                if (urlConnection.getResponseMessage() != null) {
                    errorMessage.append(' ').append(urlConnection.getResponseMessage());
                }
                if (es != null) {
                    errorMessage.append(" - ");
                    int n;
                    while (IOUtils.EOF != (n = es.read(buffer))) {
                        errorMessage.append(new String(buffer, 0, n, CHARSET));
                    }
                }
            }
            if (urlConnection.getResponseCode() > -1) {
                throw new IOException(errorMessage.toString());
            } else {
                throw e;
            }
        }
    }
}
