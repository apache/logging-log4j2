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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Objects;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Verifies that, for a {@code strict} XML configuration, the schema and the resources it imports are resolved through
 * {@link ConfigurationSource} (so they honor the Log4j URI conventions and the {@code ALLOWED_PROTOCOLS} restrictions),
 * and that a validation failure breaks the configuration by throwing a {@link ConfigurationException}.
 */
class XmlConfigurationSchemaTest {

    private static void load(final String name) {
        final URI uri;
        try {
            uri = Objects.requireNonNull(
                            XmlConfigurationSchemaTest.class.getResource("/XmlConfigurationSchemaTest/" + name))
                    .toURI();
        } catch (final Exception e) {
            throw new AssertionError("Cannot locate test resource " + name, e);
        }
        new XmlConfiguration(new LoggerContext("test"), ConfigurationSource.fromUri(uri));
    }

    /**
     * A conforming configuration validates cleanly. This also proves that the schema's {@code xsd:include} is resolved
     * through the resolver: {@code main.xsd} only compiles if {@code appenders.xsd} is found.
     */
    @Test
    void validConfigurationValidates() {
        assertThatCode(() -> load("valid.xml")).doesNotThrowAnyException();
    }

    /**
     * A configuration that violates the schema is rejected. The schema allows only a {@code Console} appender, but the
     * configuration declares a {@code File} appender.
     */
    @Test
    void invalidConfigurationIsRejected() {
        assertThatThrownBy(() -> load("invalid.xml")).isInstanceOf(ConfigurationException.class);
    }

    /**
     * Because the schema's {@code xsd:include} resources are resolved through {@link ConfigurationSource}, they obey
     * the {@code ALLOWED_PROTOCOLS} restrictions. With {@code http} excluded, an {@code http} include is rejected and
     * the schema cannot be built. The {@code @Timeout} guards against the resolver reaching the network instead of
     * rejecting the protocol.
     */
    @Test
    @Timeout(5)
    @SetTestProperty(key = "log4j2.configurationAllowedProtocols", value = "file")
    void schemaIncludeRespectsAllowedProtocols() {
        assertThatThrownBy(() -> load("http-include-config.xml")).isInstanceOf(ConfigurationException.class);
    }
}
