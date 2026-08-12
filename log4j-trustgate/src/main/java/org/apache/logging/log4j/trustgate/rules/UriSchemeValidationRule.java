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
package org.apache.logging.log4j.trustgate.rules;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.apache.logging.log4j.trustgate.spi.ValidationRule;

/**
 * Rejects URI schemes outside the configured whitelist for {@link InputType#URI_SCHEME} and
 * {@link InputType#CONFIGURATION_VALUE} inputs.
 */
public final class UriSchemeValidationRule implements ValidationRule {

    static final String ALLOWED_SCHEMES_PROPERTY = "log4j2.trustgate.allowedSchemes";
    private static final String RULE_NAME = "uri-scheme";
    private static final Set<String> DEFAULT_ALLOWED_SCHEMES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("file", "http", "https", "classpath", "classloader")));

    private final Set<String> allowedSchemes;

    public UriSchemeValidationRule() {
        this(parseAllowedSchemes(System.getProperty(ALLOWED_SCHEMES_PROPERTY)));
    }

    UriSchemeValidationRule(final Set<String> allowedSchemes) {
        this.allowedSchemes = Collections.unmodifiableSet(new HashSet<>(allowedSchemes));
    }

    @Override
    public boolean matches(final String input, final InputType type) {
        if (type != InputType.URI_SCHEME && type != InputType.CONFIGURATION_VALUE) {
            return false;
        }
        if (type == InputType.CONFIGURATION_VALUE && input.indexOf(':') < 0) {
            return false;
        }
        try {
            final String scheme = type == InputType.URI_SCHEME ? input : new URI(input).getScheme();
            if (scheme == null || scheme.isEmpty()) {
                return type == InputType.URI_SCHEME;
            }
            return !allowedSchemes.contains(scheme.toLowerCase(Locale.ROOT));
        } catch (final URISyntaxException ex) {
            return true;
        }
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    private static Set<String> parseAllowedSchemes(final String propertyValue) {
        if (propertyValue == null || propertyValue.isEmpty()) {
            return DEFAULT_ALLOWED_SCHEMES;
        }
        final Set<String> schemes = new HashSet<>();
        for (final String token : propertyValue.split(",")) {
            final String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                schemes.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }
        return schemes.isEmpty() ? DEFAULT_ALLOWED_SCHEMES : schemes;
    }
}
