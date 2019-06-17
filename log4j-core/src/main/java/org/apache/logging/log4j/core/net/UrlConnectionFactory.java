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
package org.apache.logging.log4j.core.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.net.ssl.LaxHostnameVerifier;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfigurationFactory;
import org.apache.logging.log4j.core.util.AuthorizationProvider;

/**
 * Constructs an HTTPURLConnection. This class should be considered to be internal
 */
public class UrlConnectionFactory {

    private static int DEFAULT_TIMEOUT = 60000;
    private static int connectTimeoutMillis = DEFAULT_TIMEOUT;
    private static int readTimeoutMillis = DEFAULT_TIMEOUT;
    private static final String JSON = "application/json";
    private static final String XML = "application/xml";
    private static final String PROPERTIES = "text/x-java-properties";
    private static final String TEXT = "text/plain";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    public static HttpURLConnection createConnection(URL url, long lastModifiedMillis, SslConfiguration sslConfiguration)
        throws IOException {
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        AuthorizationProvider provider = ConfigurationFactory.getAuthorizationProvider();
        if (provider != null) {
            provider.addAuthorization(urlConnection);
        }
        urlConnection.setAllowUserInteraction(false);
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setRequestMethod("GET");
        if (connectTimeoutMillis > 0) {
            urlConnection.setConnectTimeout(connectTimeoutMillis);
        }
        if (readTimeoutMillis > 0) {
            urlConnection.setReadTimeout(readTimeoutMillis);
        }
        String[] fileParts = url.getFile().split("\\.");
        String type = fileParts[fileParts.length - 1].trim();
        String contentType = isXml(type) ? XML : isJson(type) ? JSON : isProperties(type) ? PROPERTIES : TEXT;
        urlConnection.setRequestProperty("Content-Type", contentType);
        if (lastModifiedMillis > 0) {
            urlConnection.setIfModifiedSince(lastModifiedMillis);
        }
        if (url.getProtocol().equals(HTTPS) && sslConfiguration != null) {
            ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslConfiguration.getSslSocketFactory());
            if (!sslConfiguration.isVerifyHostName()) {
                ((HttpsURLConnection) urlConnection).setHostnameVerifier(LaxHostnameVerifier.INSTANCE);
            }
        }
        return urlConnection;
    }

    public static URLConnection createConnection(URL url) throws IOException {
        URLConnection urlConnection = null;
        if (url.getProtocol().equals(HTTPS) || url.getProtocol().equals(HTTP)) {
            urlConnection = createConnection(url, 0, SslConfigurationFactory.getSslConfiguration());
        } else {
            urlConnection = url.openConnection();
        }
        return urlConnection;
    }


    private static boolean isXml(String type) {
        return type.equalsIgnoreCase("xml");
    }

    private static boolean isJson(String type) {
        return type.equalsIgnoreCase("json") || type.equalsIgnoreCase("jsn");
    }

    private static boolean isProperties(String type) {
        return type.equalsIgnoreCase("properties");
    }
}
