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
import org.apache.logging.log4j.trustgate.spi.InputType;
import org.apache.logging.log4j.trustgate.spi.ValidationRule;

/**
 * Rejects JNDI lookup names whose URI scheme is present and not {@code java}, mirroring
 * {@code JndiManager#lookup(String)} behavior.
 */
public final class JndiSchemeValidationRule implements ValidationRule {

    private static final String RULE_NAME = "jndi-scheme";
    private static final String JAVA_SCHEME = "java";

    @Override
    public boolean matches(final String input, final InputType type) {
        if (type != InputType.JNDI_LOOKUP) {
            return false;
        }
        try {
            final URI uri = new URI(input);
            final String scheme = uri.getScheme();
            if (scheme == null) {
                return false;
            }
            return !JAVA_SCHEME.equals(scheme);
        } catch (final URISyntaxException ex) {
            return true;
        }
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }
}
