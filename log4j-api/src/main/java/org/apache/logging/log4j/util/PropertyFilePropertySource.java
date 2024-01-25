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
package org.apache.logging.log4j.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * PropertySource backed by a properties file. Follows the same conventions as {@link PropertiesPropertySource}.
 *
 * @since 2.10.0
 */
public class PropertyFilePropertySource extends PropertiesPropertySource {

    private static final Logger LOGGER = StatusLogger.getLogger();

    public PropertyFilePropertySource(final String fileName) {
        this(fileName, true);
    }

    public PropertyFilePropertySource(final String fileName, final boolean useTccl) {
        super(loadPropertiesFile(fileName, useTccl));
    }

    @SuppressFBWarnings(
            value = "URLCONNECTION_SSRF_FD",
            justification = "This property source should only be used with hardcoded file names.")
    private static Properties loadPropertiesFile(final String fileName, final boolean useTccl) {
        final Properties props = new Properties();
        for (final URL url : LoaderUtil.findResources(fileName, useTccl)) {
            try (final InputStream in = url.openStream()) {
                props.load(in);
            } catch (final IOException error) {
                LOGGER.error("Unable to read URL `{}`", url, error);
            }
        }
        return props;
    }
}
