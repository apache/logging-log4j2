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
package org.apache.logging.log4j.core.lookup;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Lookup properties of Log4j
 */
@Plugin(name = "log4j", category = StrLookup.CATEGORY)
public class Log4jLookup extends AbstractConfigurationAwareLookup {

    public static final String KEY_CONFIG_LOCATION = "configLocation";
    public static final String KEY_CONFIG_PARENT_LOCATION = "configParentLocation";

    private static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();

    private static String asPath(final URI uri) {
        if (uri.getScheme() == null || uri.getScheme().equals("file")) {
            return uri.getPath();
        }
        return uri.toString();
    }

    private static URI getParent(final URI uri) throws URISyntaxException {
        final String s = uri.toString();
        final int offset = s.lastIndexOf('/');
        if (offset > -1) {
            return new URI(s.substring(0, offset));
        }
        return new URI("../");
    }

    @Override
    public String lookup(final LogEvent event, final String key) {
        if (configuration != null) {
            final ConfigurationSource configSrc = configuration.getConfigurationSource();
            final File file = configSrc.getFile();
            if (file != null) {
                switch (key) {
                    case KEY_CONFIG_LOCATION:
                        return file.getAbsolutePath();

                    case KEY_CONFIG_PARENT_LOCATION:
                        return file.getParentFile().getAbsolutePath();

                    default:
                        return null;
                }
            }

            final URL url = configSrc.getURL();
            if (url != null) {
                try {
                    switch (key) {
                        case KEY_CONFIG_LOCATION:
                            return asPath(url.toURI());

                        case KEY_CONFIG_PARENT_LOCATION:
                            return asPath(getParent(url.toURI()));

                        default:
                            return null;
                    }
                } catch (final URISyntaxException use) {
                    LOGGER.error(use);
                }
            }
        }

        return null;
    }
}
