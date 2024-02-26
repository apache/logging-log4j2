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
package org.apache.logging.log4j.kubernetes;

import io.fabric8.kubernetes.client.Config;
import java.time.Duration;
import java.util.function.Supplier;
import org.apache.logging.log4j.kit.env.PropertyEnvironment;

/**
 * Obtains properties used to configure the Kubernetes client.
 */
public class KubernetesClientProperties {

    private static final String[] PREFIXES = {"log4j2.kubernetes.client.", "spring.cloud.kubernetes.client."};
    private static final String API_VERSION = "apiVersion";
    private static final String CA_CERT_FILE = "caCertFile";
    private static final String CA_CERT_DATA = "caCertData";
    private static final String CLIENT_CERT_FILE = "clientCertFile";
    private static final String CLIENT_CERT_DATA = "clientCertData";
    private static final String CLIENT_KEY_FILE = "clientKeyFile";
    private static final String CLIENT_KEY_DATA = "clientKeyData";
    private static final String CLIENT_KEY_ALGO = "clientKeyAlgo";
    private static final String CLIENT_KEY_PASSPHRASE = "clientKeyPassphrase";
    private static final String CONNECTION_TIMEOUT = "connectionTimeout";
    private static final String HTTP_PROXY = "httpProxy";
    private static final String HTTPS_PROXY = "httpsProxy";
    private static final String LOGGING_INTERVAL = "loggingInterval";
    private static final String MASTER_URL = "masterUrl";
    private static final String NAMESPACE = "namespace";
    private static final String NO_PROXY = "noProxy";
    private static final String PASSWORD = "password";
    private static final String PROXY_USERNAME = "proxyUsername";
    private static final String PROXY_PASSWORD = "proxyPassword";
    private static final String REQUEST_TIMEOUT = "requestTimeout";
    private static final String ROLLING_TIMEOUT = "rollingTimeout";
    private static final String TRUST_CERTS = "trustCerts";
    private static final String USERNAME = "username";
    private static final String WATCH_RECONNECT_INTERVAL = "watchReconnectInterval";
    private static final String WATCH_RECONNECT_LIMIT = "watchReconnectLimit";

    private final PropertyEnvironment props = PropertyEnvironment.getGlobal();
    private final Config base;

    public KubernetesClientProperties(final Config base) {
        this.base = base;
    }

    private String getStringProperty(final String property, final Supplier<String> fallback) {
        for (final String prefix : PREFIXES) {
            final String value = props.getStringProperty(prefix + property);
            if (value != null) {
                return value;
            }
        }
        return fallback.get();
    }

    public String getApiVersion() {
        return getStringProperty(API_VERSION, base::getApiVersion);
    }

    public String getCaCertFile() {
        return getStringProperty(CA_CERT_FILE, base::getCaCertFile);
    }

    public String getCaCertData() {
        return getStringProperty(CA_CERT_DATA, base::getCaCertData);
    }

    public String getClientCertFile() {
        return getStringProperty(CLIENT_CERT_FILE, base::getClientCertFile);
    }

    public String getClientCertData() {
        return getStringProperty(CLIENT_CERT_DATA, base::getClientCertData);
    }

    public String getClientKeyFile() {
        return getStringProperty(CLIENT_KEY_FILE, base::getClientKeyFile);
    }

    public String getClientKeyData() {
        return getStringProperty(CLIENT_KEY_DATA, base::getClientKeyData);
    }

    public String getClientKeyAlgo() {
        return getStringProperty(CLIENT_KEY_ALGO, base::getClientKeyAlgo);
    }

    public String getClientKeyPassphrase() {
        return getStringProperty(CLIENT_KEY_PASSPHRASE, base::getClientKeyPassphrase);
    }

    public int getConnectionTimeout() {
        final String stringProperty = getStringProperty(CONNECTION_TIMEOUT, null);
        final Duration timeout = stringProperty != null ? Duration.parse(stringProperty) : null;
        if (timeout != null) {
            return (int) timeout.toMillis();
        }
        return base.getConnectionTimeout();
    }

    public String getHttpProxy() {
        return getStringProperty(HTTP_PROXY, base::getHttpProxy);
    }

    public String getHttpsProxy() {
        return getStringProperty(HTTPS_PROXY, base::getHttpsProxy);
    }

    public int getLoggingInterval() {
        final String stringProperty = getStringProperty(CONNECTION_TIMEOUT, null);
        final Duration interval = stringProperty != null ? Duration.parse(stringProperty) : null;
        if (interval != null) {
            return (int) interval.toMillis();
        }
        return base.getLoggingInterval();
    }

    public String getMasterUrl() {
        return getStringProperty(MASTER_URL, base::getMasterUrl);
    }

    public String getNamespace() {
        return getStringProperty(NAMESPACE, base::getNamespace);
    }

    public String[] getNoProxy() {
        final String result = getStringProperty(NO_PROXY, null);
        if (result != null) {
            return result.replace("\\s", "").split(",");
        }
        return base.getNoProxy();
    }

    public String getPassword() {
        return getStringProperty(PASSWORD, base::getPassword);
    }

    public String getProxyUsername() {
        return getStringProperty(PROXY_USERNAME, base::getProxyUsername);
    }

    public String getProxyPassword() {
        return getStringProperty(PROXY_PASSWORD, base::getProxyPassword);
    }

    public int getRequestTimeout() {
        final Duration interval = Duration.parse(getStringProperty(REQUEST_TIMEOUT, null));
        if (interval != null) {
            return (int) interval.toMillis();
        }
        return base.getRequestTimeout();
    }

    public long getRollingTimeout() {
        final Duration interval = Duration.parse(getStringProperty(ROLLING_TIMEOUT, null));
        if (interval != null) {
            return interval.toMillis();
        }
        return base.getRollingTimeout();
    }

    public Boolean isTrustCerts() {
        final String stringProperty = getStringProperty(TRUST_CERTS, () -> null);
        return stringProperty != null
                ? Boolean.parseBoolean(stringProperty)
                : ((Supplier<Boolean>) base::isTrustCerts).get();
    }

    public String getUsername() {
        return getStringProperty(USERNAME, base::getUsername);
    }

    public int getWatchReconnectInterval() {
        final Duration interval = Duration.parse(getStringProperty(WATCH_RECONNECT_INTERVAL, null));
        if (interval != null) {
            return (int) interval.toMillis();
        }
        return base.getWatchReconnectInterval();
    }

    public int getWatchReconnectLimit() {
        final Duration interval = Duration.parse(getStringProperty(WATCH_RECONNECT_LIMIT, null));
        if (interval != null) {
            return (int) interval.toMillis();
        }
        return base.getWatchReconnectLimit();
    }
}
