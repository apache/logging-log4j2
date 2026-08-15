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
package org.apache.logging.log4j.spring.cloud.config.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "spring.cloud.config.watch.enabled")
public class Log4j2EventListener implements ApplicationListener<EnvironmentChangeEvent>, EnvironmentAware {
    private static Logger LOGGER = LogManager.getLogger(Log4j2EventListener.class);
    private Environment environment;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(final EnvironmentChangeEvent environmentChangeEvent) {
        if (!isWatchEnabled(environmentChangeEvent)) {
            LOGGER.debug("Ignoring environment change event; spring.cloud.config.watch.enabled is false");
            return;
        }
        LOGGER.debug("Application change event triggered");
        WatchEventManager.publishEvent();
    }

    /**
     * {@code spring.factories} constructs this listener outside the bean factory, so
     * {@code @ConditionalOnProperty} never applies. Honor the same property here.
     */
    private boolean isWatchEnabled(final EnvironmentChangeEvent event) {
        Environment env = this.environment;
        if (env == null) {
            final Object source = event.getSource();
            if (source instanceof ApplicationContext) {
                env = ((ApplicationContext) source).getEnvironment();
            }
        }
        if (env == null) {
            return true;
        }
        return !Boolean.FALSE.equals(env.getProperty("spring.cloud.config.watch.enabled", Boolean.class));
    }
}
