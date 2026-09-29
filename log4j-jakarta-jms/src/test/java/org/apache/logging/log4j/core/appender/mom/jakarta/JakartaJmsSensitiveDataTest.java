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
package org.apache.logging.log4j.core.appender.mom.jakarta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import javax.naming.Context;
import org.junit.Test;

public class JakartaJmsSensitiveDataTest {

    @Test
    public void builderToStringMasksSecurityCredentials() {
        final String credentials = "jndi-secret";

        final JmsAppender.Builder builder = JmsAppender.newBuilder();
        builder.setName("JMS");
        builder.setSecurityCredentials(credentials);
        final String value = builder.toString();

        assertThat(value).doesNotContain(credentials).contains("securityCredentials=*****");
    }

    @Test
    public void managerConfigurationToStringMasksSecurityCredentials() {
        final String credentials = "jndi-secret";
        final String password = "jms-password";
        final Properties jndiProperties = new Properties();
        jndiProperties.setProperty(Context.SECURITY_CREDENTIALS, credentials);
        jndiProperties.setProperty(Context.PROVIDER_URL, "test-provider");

        final JmsManager.JmsManagerConfiguration configuration = new JmsManager.JmsManagerConfiguration(
                jndiProperties, "connectionFactory", "destination", "user", password.toCharArray(), false, 0);

        assertThat(configuration.toString())
                .doesNotContain(credentials)
                .doesNotContain(password)
                .contains(Context.SECURITY_CREDENTIALS + "=*****")
                .contains(Context.PROVIDER_URL + "=test-provider");

        assertThat(jndiProperties.getProperty(Context.SECURITY_CREDENTIALS)).isEqualTo(credentials);
    }
}
