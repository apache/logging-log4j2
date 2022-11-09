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
import java.util.Base64;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;

/**
 * Provides the Basic Authorization header to a request.
 */
public class BasicAuthorizationProvider implements AuthorizationProvider {
    private static final String[] PREFIXES = {"log4j2.config.", "log4j2.Configuration.", "logging.auth."};
    private static final String AUTH_USER_NAME = "username";
    private static final String AUTH_PASSWORD = "password";
    private static final String AUTH_PASSWORD_DECRYPTOR = "passwordDecryptor";
    public static final String CONFIG_USER_NAME = "log4j2.configurationUserName";
    public static final String CONFIG_PASSWORD = "log4j2.configurationPassword";
    public static final String PASSWORD_DECRYPTOR = "log4j2.passwordDecryptor";

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Base64.Encoder encoder = Base64.getEncoder();

    private String authString = null;

    public BasicAuthorizationProvider(final PropertyEnvironment props) {
        final String userName = props.getStringProperty(PREFIXES,AUTH_USER_NAME,
                () -> props.getStringProperty(CONFIG_USER_NAME));
        String password = props.getStringProperty(PREFIXES, AUTH_PASSWORD,
                () -> props.getStringProperty(CONFIG_PASSWORD));
        final String decryptor = props.getStringProperty(PREFIXES, AUTH_PASSWORD_DECRYPTOR,
                () -> props.getStringProperty(PASSWORD_DECRYPTOR));
        if (decryptor != null) {
            try {
                final Object obj = LoaderUtil.newInstanceOf(decryptor);
                if (obj instanceof PasswordDecryptor) {
                    password = ((PasswordDecryptor) obj).decryptPassword(password);
                }
            } catch (final Exception ex) {
                LOGGER.warn("Unable to decrypt password.", ex);
            }
        }
        if (userName != null && password != null) {
            authString = "Basic " + encoder.encodeToString((userName + ":" + password).getBytes());
        }
    }

    @Override
    public void addAuthorization(final URLConnection urlConnection) {
        if (authString != null) {
            urlConnection.setRequestProperty("Authorization", authString);
        }
    }
}
