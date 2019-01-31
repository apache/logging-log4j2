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
package org.apache.logging.log4j.spring.cloud.config.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listen for events indicating the remote configuration has changed.
 */
@Component
@ConditionalOnClass(ContextRefresher.class)
@ConditionalOnBean(ContextRefresher.class)
@ConditionalOnProperty(value = "spring.cloud.config.watch.enabled")
public class Log4j2EventListener {

	private static Logger LOGGER = LogManager.getLogger(Log4j2EventListener.class);

	@EventListener(EnvironmentChangeEvent.class)
	public void handleEnvironmentChangeEvent(EnvironmentChangeEvent event) {
		LOGGER.debug("Environment change event received");
		WatchEventManager.publishEvent();
	}
}
