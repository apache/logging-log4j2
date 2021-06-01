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
package org.apache.logging.log4j.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * PropertySource backed by a properties file. Follows the same conventions as {@link PropertiesPropertySource}.
 *
 * @since 2.10.0
 */
public class PropertyFilePropertySource extends PropertiesPropertySource {

    public PropertyFilePropertySource(final String fileName) {
        super(loadPropertiesFile(fileName));
    }

    private static Properties loadPropertiesFile(final String fileName) {
        final Properties props = new Properties();
        for (final URL url : LoaderUtil.findResources(fileName)) {
            try (final InputStream in = url.openStream()) {
                props.load(in);
            } catch (final IOException e) {
                LowLevelLogUtil.logException("Unable to read " + url, e);
            }
        }
        return props;
    }

    @Override
    public int getPriority() {
        return 0;
    }

}
