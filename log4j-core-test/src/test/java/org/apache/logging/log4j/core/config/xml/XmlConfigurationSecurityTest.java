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
package org.apache.logging.log4j.core.config.xml;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Verifies the contract of {@link XmlConfiguration} regarding the resolution of external resources.
 *
 * <p>Configuration files are part of the trusted computing base (see the
 * <a href="https://logging.apache.org/security.html">Log4j security policy</a>); the hardenings checked here are
 * defense in depth, not a security boundary against a malicious configuration. These tests only cover what Log4j is
 * responsible for:</p>
 * <ul>
 *     <li>a forbidden external fetch is surfaced as a {@link ConfigurationException} (rather than a half-built
 *         configuration), and</li>
 *     <li>XInclude resources are subject to the same {@code ALLOWED_PROTOCOLS} restrictions as the configuration
 *         file itself.</li>
 * </ul>
 *
 * <p>All external fetches are forbidden; whether a forbidden fetch raises an error or is silently skipped is part of
 * the contract of the {@code eu.copernik:copernik-xml-factory} library and is tested there.</p>
 */
@Tag("functional")
@Tag("security")
class XmlConfigurationSecurityTest {

    private static final ConfigurationFactory FACTORY = new XmlConfigurationFactory();

    private static Configuration getConfiguration(final String resource) {
        return FACTORY.getConfiguration(null, null, URI.create("classpath:XmlConfigurationSecurity/" + resource));
    }

    /**
     * A configuration that triggers a forbidden external fetch fails to load with a {@link ConfigurationException}.
     * The {@code @Timeout} guards against an SSRF: a fetch attempt against the unreachable host would block until the
     * connection times out.
     */
    @Test
    @Timeout(5)
    void forbiddenExternalFetchThrowsConfigurationException() {
        assertThatThrownBy(() -> getConfiguration("external-parameter-entity.xml"))
                .isInstanceOf(ConfigurationException.class);
    }

    /**
     * XInclude resources are resolved through {@code ConfigurationSource}, so they are subject to the same
     * {@code ALLOWED_PROTOCOLS} restrictions as the configuration file. With {@code http} excluded, an {@code http}
     * include cannot be resolved and the configuration fails to load.
     */
    @Test
    @Timeout(5)
    @SetTestProperty(key = "log4j2.configurationEnableXInclude", value = "true")
    @SetTestProperty(key = "log4j2.configurationAllowedProtocols", value = "file")
    void xIncludeRespectsAllowedProtocols() {
        assertThatThrownBy(() -> getConfiguration("xinclude.xml")).isInstanceOf(ConfigurationException.class);
    }
}
