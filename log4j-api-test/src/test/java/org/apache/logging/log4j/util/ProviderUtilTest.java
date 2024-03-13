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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.apache.logging.log4j.TestProvider;
import org.apache.logging.log4j.test.TestLoggerContextFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ProviderUtilTest {

    /*
     * Force initialization of ProviderUtil#PROVIDERS
     */
    static {
        ProviderUtil.lazyInit();
    }

    @Test
    void should_select_provider_with_highest_priority() {
        final PropertiesUtil properties = new PropertiesUtil(new Properties());
        assertThat(ProviderUtil.selectProvider(properties))
                .as("check selected provider")
                .isInstanceOf(TestProvider.class);
    }

    @Test
    void should_recognize_log4j_provider_property() {
        final Properties map = new Properties();
        map.setProperty("log4j.provider", LocalProvider.class.getName());
        final PropertiesUtil properties = new PropertiesUtil(map);
        assertThat(ProviderUtil.selectProvider(properties))
                .as("check selected provider")
                .isInstanceOf(LocalProvider.class);
    }

    @Test
    void should_recognize_log4j_factory_property() {
        final Properties map = new Properties();
        map.setProperty("log4j2.loggerContextFactory", LocalLoggerContextFactory.class.getName());
        final PropertiesUtil properties = new PropertiesUtil(map);
        assertThat(ProviderUtil.selectProvider(properties).getLoggerContextFactory())
                .as("check selected logger context factory")
                .isInstanceOf(LocalLoggerContextFactory.class);
    }

    public static class LocalLoggerContextFactory extends TestLoggerContextFactory {}

    /**
     * A provider with a smaller priority than {@link org.apache.logging.log4j.TestProvider}.
     */
    public static class LocalProvider extends org.apache.logging.log4j.spi.Provider {
        public LocalProvider() {
            super(0, CURRENT_VERSION, LocalLoggerContextFactory.class);
        }
    }
}
