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
package org.apache.logging.log4j.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.junit.jupiter.api.Test;

class LoggerContextAwarePostProcessorTest {
    static class TestBean implements LoggerContextAware {
        LoggerContext context;

        @Override
        public void setLoggerContext(final LoggerContext loggerContext) {
            context = loggerContext;
        }
    }

    @Test
    void loggerContextAwareInjection() {
        try (final LoggerContext context = new LoggerContext(getClass().getName())) {
            final InstanceFactory instanceFactory = context.getInstanceFactory();
            final TestBean instance = instanceFactory.getInstance(TestBean.class);
            assertThat(instance.context).isSameAs(context);
        }
    }
}
