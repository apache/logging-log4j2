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
package org.apache.logging.log4j.kubernetes;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Builds a Kubernetes Client.
 */
public class KubernetesClientBuilder {

    private static final Logger LOGGER = StatusLogger.getLogger();

    public KubernetesClient createClient() {
        Config config = kubernetesClientConfig();
        return config != null ? new DefaultKubernetesClient(config) : null;
    }

    private Config kubernetesClientConfig() {
        Config base = null;
        try {
            base = Config.autoConfigure(null);
        } catch (Exception ex) {
            if (ex instanceof  NullPointerException) {
                return null;
            }
        }
        KubernetesClientProperties props = new KubernetesClientProperties(base);
        Config properties = new ConfigBuilder(base)
                .withApiVersion(props.getApiVersion())
                .withCaCertData(props.getCaCertData())
                .withCaCertFile(props.getCaCertFile())
                .withClientCertData(props.getClientCertData())
                .withClientCertFile(props.getClientCertFile())
                .withClientKeyAlgo(props.getClientKeyAlgo())
                .withClientKeyData(props.getClientKeyData())
                .withClientKeyFile(props.getClientKeyFile())
                .withClientKeyPassphrase(props.getClientKeyPassphrase())
                .withConnectionTimeout(props.getConnectionTimeout())
                .withHttpProxy(props.getHttpProxy())
                .withHttpsProxy(props.getHttpsProxy())
                .withMasterUrl(props.getMasterUrl())
                .withNamespace(props.getNamespace())
                .withNoProxy(props.getNoProxy())
                .withPassword(props.getPassword())
                .withProxyPassword(props.getProxyPassword())
                .withProxyUsername(props.getProxyUsername())
                .withRequestTimeout(props.getRequestTimeout())
                .withRollingTimeout(props.getRollingTimeout())
                .withTrustCerts(props.isTrustCerts())
                .withUsername(props.getUsername())
                .build();
        return properties;
    }
}
