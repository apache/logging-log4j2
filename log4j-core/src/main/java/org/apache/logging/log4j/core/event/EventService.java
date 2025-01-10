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
package org.apache.logging.log4j.core.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service for handling events and subscriptions to events
 */
public class EventService {

    private static Map<EventListener, EventFilter> eventListeners = new ConcurrentHashMap<>();

    /**
     * Subscribe to receive events
     * @param eventListener the listener the events should be sent to
     * @param eventFilter a filter to control the events to be sent to the listener
     */
    public static void subscribe(final EventListener eventListener, final EventFilter eventFilter) {
        eventListeners.put(eventListener, eventFilter);
    }

    /**
     * Publish the given event to subscribed listeners
     *
     * @param event the event to publish
     */
    public static void publish(Event event) {
        System.out.println("Publishing event " + event);

        eventListeners.entrySet().stream()
                .filter(listener -> listener.getValue().matches(event))
                .forEach(listener -> {
                    System.out.println("To:" + listener.getKey());
                    listener.getKey().onEvent(event);
                });
    }
}
