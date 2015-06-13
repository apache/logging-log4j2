/*
 * Copyright 2015 Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ContextAnchor;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Lookup properties of Log4j
 */
@Plugin(name = "log4j", category = StrLookup.CATEGORY)
public class Log4jLookup extends AbstractLookup {

    public final static String KEY_CONFIG_LOCATION = "configLocation";
    public final static String KEY_CONFIG_PARENT_LOCATION = "configParentLocation";

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
        final LoggerContext ctx = ContextAnchor.THREAD_CONTEXT.get();
        if (ctx == null) {
            return null;
        }

        switch (key) {
        case KEY_CONFIG_LOCATION:
            return asPath(ctx.getConfigLocation());

        case KEY_CONFIG_PARENT_LOCATION:
            try {
                return asPath(getParent(ctx.getConfigLocation()));
            } catch (final URISyntaxException use) {
                LOGGER.error(use);
                return null;
            }

        default:
            return null;
        }
    }
}
