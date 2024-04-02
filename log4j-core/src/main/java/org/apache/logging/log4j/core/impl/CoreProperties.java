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
package org.apache.logging.log4j.core.impl;

import java.net.URI;
import java.nio.file.Path;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ReliabilityStrategy;
import org.apache.logging.log4j.core.config.composite.MergeStrategy;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.PasswordDecryptor;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.kit.env.Log4jProperty;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.util.StringMap;
import org.jspecify.annotations.Nullable;

public final class CoreProperties {

    private CoreProperties() {}

    @Log4jProperty(name = "async")
    public record AsyncProperties(boolean formatMessagesInBackground) {}

    @Log4jProperty(name = "async.queueFullPolicy")
    public record QueueFullPolicyProperties(
            @Nullable String type, @Log4jProperty(defaultValue = "INFO") Level discardThreshold) {
        public static QueueFullPolicyProperties defaultValue() {
            return new QueueFullPolicyProperties(null, Level.INFO);
        }

        public QueueFullPolicyProperties withType(final @Nullable String type) {
            return new QueueFullPolicyProperties(type, discardThreshold);
        }

        public QueueFullPolicyProperties withLevel(final Level discardThreshold) {
            return new QueueFullPolicyProperties(type, discardThreshold);
        }
    }

    /**
     * @param provider The {@link AuthorizationProvider} to use for HTTP requests or {@code null}.
     * @param basic Authentication data for HTTP Basic authentication.
     */
    @Log4jProperty(name = "auth")
    public record AuthenticationProperties(
            @Nullable Class<? extends AuthorizationProvider> provider, BasicAuthenticationProperties basic) {}

    /**
     * Configuration properties for HTTP Basic authentication.
     *
     * @param username The username.
     * @param password A possibly encrypted password.
     * @param passwordDecryptor
     */
    public record BasicAuthenticationProperties(
            String username, String password, Class<? extends PasswordDecryptor> passwordDecryptor) {}

    /**
     * Properties related to the retrieval of a configuration.
     *
     * @param allowedProtocols The protocols allowed for the configuration location.
     * @param clock A custom {@link Clock} implementation to use to timestamp log events. The supported values are:
     *              <ul>
     *                  <li>{@code SystemMillisClock}</li>
     *                  <li>{@code CachedClock}</li>
     *                  <li>{@code CoarseCachedClock}</li>
     *              </ul>
     *              <p>If {@code null}, the system clock will be used.</p>
     * @param factory The {@link ConfigurationFactory} to use or {@code  null} for the default one.
     * @param level The default level of the root logger.
     * @param location The location (file path or {@link URI}) of the configuration file. If {@code null} a standard set
     *             of locations is used.
     * @param mergeStrategy The {@link MergeStrategy} to use if multiple configuration files are present.
     * @param reliabilityStrategy The {@link ReliabilityStrategy} to use during the reconfiguration process.
     * @param usePreciseClock If {@code true} a clock with nanosecond precision will be used whenever available.
     * @param waitMillisBeforeStopOldConfig The number of milliseconds to wait for the old configuration to stop.
     */
    @Log4jProperty(name = "configuration")
    public record ConfigurationProperties(
            @Log4jProperty(defaultValue = "file,https,jar") String allowedProtocols,
            @Nullable String clock,
            @Nullable Class<? extends ConfigurationFactory> factory,
            @Log4jProperty(defaultValue = "ERROR") Level level,
            @Nullable String location,
            @Nullable Class<? extends MergeStrategy> mergeStrategy,
            @Log4jProperty(defaultValue = "AwaitCompletion") String reliabilityStrategy,
            boolean usePreciseClock,
            @Log4jProperty(defaultValue = "5000") long waitMillisBeforeStopOldConfig) {}

    /**
     * Properties to tune console output.
     */
    @Log4jProperty(name = "console")
    public record ConsoleProperties(@Nullable Boolean jansiEnabled) {}

    /**
     * Properties to tune garbage collection.
     */
    @Log4jProperty(name = "gc")
    public record GarbageCollectionProperties(
            @Log4jProperty(defaultValue = "8192") int encoderByteBufferSize,
            @Log4jProperty(defaultValue = "2048") int encoderCharBufferSize,
            @Log4jProperty(defaultValue = "true") boolean enableDirectEncoders,
            @Log4jProperty(defaultValue = "128") int initialReusableMessageSize,
            @Log4jProperty(defaultValue = "518") int maxReusableMessageSize,
            @Log4jProperty(defaultValue = "2048") int layoutStringBuilderMaxSize) {}

    /**
     * @param algorithm The {@link javax.net.ssl.KeyManagerFactory} algorithm.
     */
    public record KeyManagerFactoryProperties(@Nullable String algorithm) {}

    /**
     * A container for keystore configuration data.
     *
     * @param keyManagerFactory Configuration of the {@link javax.net.ssl.KeyManagerFactory}.
     * @param location The location of the key store.
     * @param password The password as characters.
     * @param passwordEnvVar The environment variable containing the password.
     * @param passwordFile The file containing the password.
     * @param type The type of keystore.
     */
    public record KeyStoreProperties(
            KeyManagerFactoryProperties keyManagerFactory,
            @Nullable String location,
            char @Nullable [] password,
            @Nullable String passwordEnvVar,
            @Nullable Path passwordFile,
            @Nullable String type) {
        public static KeyStoreProperties defaultValue() {
            return new KeyStoreProperties(null, null, null, null, null, null);
        }
    }

    @Log4jProperty(name = "loader")
    public record LoaderProperties(boolean ignoreTccl) {}

    /**
     * @param contextData Configuration for the context data.
     * @param factory The {@link LogEventFactory} to use.
     */
    @Log4jProperty(name = "logEvent")
    public record LogEventProperties(
            ContextDataProperties contextData, @Nullable Class<? extends LogEventFactory> factory) {}

    /**
     * @param type The {@link StringMap} class to use for context data.
     * @param injector The {@link ContextDataInjector} to use to retrieve context data.
     */
    public record ContextDataProperties(
            @Nullable Class<? extends StringMap> type, @Nullable Class<? extends ContextDataInjector> injector) {}

    @Log4jProperty(name = "loggerContext")
    public record LoggerContextProperties(
            @Nullable Class<? extends LoggerContextFactory> factory,
            @Nullable Class<? extends ContextSelector> selector,
            @Nullable Class<? extends ShutdownCallbackRegistry> shutdownCallbackRegistry,
            @Nullable Boolean shutdownHookEnabled,
            boolean stacktraceOnStart) {}

    @Log4jProperty(name = "message")
    public record MessageProperties(@Nullable Class<? extends MessageFactory> factory) {}

    /**
     * @param level The default level of the status logger.
     */
    @Log4jProperty(name = "statusLogger")
    public record StatusLoggerProperties(@Log4jProperty(defaultValue = "ERROR") Level level) {}

    @Log4jProperty(name = "threadContext")
    public record ThreadContextProperties(
            @Log4jProperty(defaultValue = "true") boolean enable,
            @Log4jProperty(defaultValue = "true") boolean enableStack,
            ThreadContextMapProperties map) {}

    /**
     * @param enable If {@code false} disables the thread context map,
     * @param type The fully-qualified class name of a {@link org.apache.logging.log4j.spi.ThreadContextMap}
     *            implementation or one of the recognized constants.
     * @see org.apache.logging.log4j.spi.Provider
     */
    public record ThreadContextMapProperties(
            @Log4jProperty(defaultValue = "true") boolean enable,
            boolean garbageFree,
            @Log4jProperty(defaultValue = "16") int initialCapacity,
            @Nullable String type) {}

    /**
     * Stores the configuration of the default SslConfiguration.
     *
     * @param trustStore The configuration of the {@link javax.net.ssl.TrustManager}.
     * @param keyStore The configuration of the {@link javax.net.ssl.KeyManager}.
     * @param verifyHostName If set to {@code true} hostname are verified.
     */
    @Log4jProperty(name = "transportSecurity")
    public record TransportSecurityProperties(
            KeyStoreProperties trustStore, KeyStoreProperties keyStore, boolean verifyHostName) {
        public static TransportSecurityProperties defaultValue() {
            return new TransportSecurityProperties(
                    KeyStoreProperties.defaultValue(), KeyStoreProperties.defaultValue(), false);
        }

        public TransportSecurityProperties withKeyStore(final KeyStoreProperties keyStore) {
            return new TransportSecurityProperties(trustStore, keyStore, verifyHostName);
        }

        public TransportSecurityProperties withTrustStore(final KeyStoreProperties trustStore) {
            return new TransportSecurityProperties(trustStore, keyStore, verifyHostName);
        }
    }

    @Log4jProperty(name = "uuid")
    public record UuidProperties(long sequence) {}

    @Log4jProperty(name = "v1")
    public record Version1Properties(String configuration, boolean compatibility) {}
}
