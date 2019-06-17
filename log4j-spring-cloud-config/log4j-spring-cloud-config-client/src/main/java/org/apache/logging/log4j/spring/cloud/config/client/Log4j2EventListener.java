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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.cloud.bus.SpringCloudBusClient;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBusEnabled
@EnableBinding(SpringCloudBusClient.class)
@ConditionalOnProperty(value = "spring.cloud.config.watch.enabled")
public class Log4j2EventListener {
    private static Logger LOGGER = LogManager.getLogger(Log4j2EventListener.class);

    @EventListener(classes = RemoteApplicationEvent.class)
    public void acceptLocal(RemoteApplicationEvent event) {
        LOGGER.debug("Refresh application event triggered");
        WatchEventManager.publishEvent();
    }

    @StreamListener(SpringCloudBusClient.INPUT)
    public void acceptRemote(RemoteApplicationEvent event) {
        LOGGER.debug("Refresh application event triggered");
        WatchEventManager.publishEvent();
    }
}
