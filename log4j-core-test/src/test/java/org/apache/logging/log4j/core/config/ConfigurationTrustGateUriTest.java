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
package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigurationTrustGateUriTest {

    @TempDir
    private Path tempDir;

    @Test
    void rejectsFtpSchemeInConfigurationSource() {
        final URI ftpUri = URI.create("ftp://example.com/log4j2.xml");
        final ConfigurationException exception =
                assertThrows(ConfigurationException.class, () -> ConfigurationSource.fromUri(ftpUri));
        assertTrue(exception.getMessage().contains("ftp"));
    }

    @Test
    void rejectsLdapSchemeInConfigurationSource() {
        final URI ldapUri = URI.create("ldap://evil.example/cn=config");
        final ConfigurationException exception =
                assertThrows(ConfigurationException.class, () -> ConfigurationSource.fromUri(ldapUri));
        assertTrue(exception.getMessage().contains("ldap"));
    }

    @Test
    void rejectsFtpSchemeInConfigurationFactory() {
        final URI ftpUri = URI.create("ftp://example.com/log4j2.xml");
        final ConfigurationException exception =
                assertThrows(ConfigurationException.class, () -> ConfigurationFactory.validateConfigurationUri(ftpUri));
        assertTrue(exception.getMessage().contains("ftp"));
    }

    @Test
    void rejectsLdapSchemeInConfigurationFactory() {
        final URI ldapUri = URI.create("ldap://evil.example/cn=config");
        final ConfigurationException exception = assertThrows(
                ConfigurationException.class, () -> ConfigurationFactory.validateConfigurationUri(ldapUri));
        assertTrue(exception.getMessage().contains("ldap"));
    }

    @Test
    void acceptsFileScheme() throws Exception {
        final Path configFile = tempDir.resolve("log4j2.xml");
        Files.write(configFile, "<Configuration status=\"OFF\"/>".getBytes(StandardCharsets.UTF_8));
        final URI fileUri = configFile.toUri();

        ConfigurationFactory.validateConfigurationUri(fileUri);
        final ConfigurationSource source = ConfigurationSource.fromUri(fileUri);
        assertNotNull(source);
        assertNotNull(source.getInputStream());
    }
}
