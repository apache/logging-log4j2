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
package org.apache.logging.log4j.core.util;

import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.plugins.ContextScoped;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertyResolver;

/**
 * Provides the Basic Authorization header to a request.
 */
@ContextScoped
public class BasicAuthorizationProvider implements AuthorizationProvider {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Base64.Encoder encoder = Base64.getEncoder();

    private String authString = null;

    @Inject
    public BasicAuthorizationProvider(final PropertyResolver resolver, final Injector injector) {
        final String user = resolver.getString(Log4jProperties.TRANSPORT_SECURITY_BASIC_USERNAME).orElse(null);
        final String pass = resolver.getString(Log4jProperties.TRANSPORT_SECURITY_BASIC_PASSWORD)
                .map(password -> injector.tryGetInstance(PasswordDecryptor.class)
                        .map(passwordDecryptor -> {
                            try {
                                return passwordDecryptor.decryptPassword(password);
                            } catch (final Exception e) {
                                LOGGER.warn("Unable to decrypt password", e);
                                return null;
                            }
                        })
                        .orElse(password))
                .orElse(null);
        if (user != null && pass != null) {
            authString = "Basic " + encoder.encodeToString((user + ':' + pass).getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void addAuthorization(final URLConnection urlConnection) {
        if (authString != null) {
            urlConnection.setRequestProperty("Authorization", authString);
        }
    }
}
