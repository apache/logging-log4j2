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
package org.apache.log4j.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * An {@link EntityResolver} specifically designed to return
 * <code>log4j.dtd</code> which is embedded within the log4j jar
 * file. Any other external resource is resolved through
 * {@link ConfigurationSource#fromUri(URI)}, the same way the configuration
 * file itself is resolved, so it is subject to the same restrictions (e.g.
 * the {@code log4j2.Configuration.allowedProtocols} property). Resources
 * that cannot be resolved this way are replaced by an empty source.
 */
public class Log4jEntityResolver implements EntityResolver {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String PUBLIC_ID = "-//APACHE//DTD LOG4J 1.2//EN";

    @Override
    public InputSource resolveEntity(final String publicId, final String systemId) {
        if ((systemId != null && systemId.endsWith("log4j.dtd")) || PUBLIC_ID.equals(publicId)) {
            final Class<?> clazz = getClass();
            InputStream in = clazz.getResourceAsStream("/org/apache/log4j/xml/log4j.dtd");
            if (in == null) {
                LOGGER.warn(
                        "Could not find [log4j.dtd] using [{}] class loader, parsed without DTD.",
                        clazz.getClassLoader());
                in = new ByteArrayInputStream(Constants.EMPTY_BYTE_ARRAY);
            }
            return new InputSource(in);
        }
        // Resolve using `ConfigurationSource`
        if (systemId != null) {
            try {
                final ConfigurationSource source = ConfigurationSource.fromUri(new URI(systemId));
                if (source != null) {
                    final InputSource inputSource = new InputSource(source.getInputStream());
                    inputSource.setSystemId(systemId);
                    return inputSource;
                }
            } catch (final URISyntaxException e) {
                LOGGER.warn("Resolution error: invalid URI {}", systemId, e);
            }
        }
        // Fall back to empty resource
        LOGGER.warn("Resolution error: unable to resolve {}", systemId);
        return new InputSource(new ByteArrayInputStream(Constants.EMPTY_BYTE_ARRAY));
    }
}
