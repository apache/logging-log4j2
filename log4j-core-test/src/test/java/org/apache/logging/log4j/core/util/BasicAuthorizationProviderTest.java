/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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
package org.apache.logging.log4j.core.util;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLConnection;
import java.util.Base64;
import java.util.Properties;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.Test;

class BasicAuthorizationProviderTest {

    @Test
    void usesLegacyEncodingPropertyWhenPrimaryPropertyIsAbsent() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty("logging.auth.username", "usér");
        properties.setProperty("logging.auth.password", "passé");
        properties.setProperty("logging.auth.encoding", ISO_8859_1.name());
        final BasicAuthorizationProvider provider = new BasicAuthorizationProvider(new PropertiesUtil(properties));

        final RecordingURLConnection connection = new RecordingURLConnection();
        provider.addAuthorization(connection);

        final String credentials = "usér:passé";
        final String expected = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(ISO_8859_1));
        assertThat(connection.requestValue).isEqualTo(expected);
    }

    private static final class RecordingURLConnection extends URLConnection {

        private String requestValue;

        private RecordingURLConnection() throws Exception {
            super(new java.net.URL("http://localhost"));
        }

        @Override
        public void connect() {}

        @Override
        public void setRequestProperty(final String key, final String value) {
            if ("Authorization".equals(key)) {
                requestValue = value;
            }
        }
    }
}
