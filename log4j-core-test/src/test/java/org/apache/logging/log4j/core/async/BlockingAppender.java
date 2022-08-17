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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Appender that can be halted and resumed, for testing queue-full scenarios.
 */
@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin("Blocking")
public class BlockingAppender extends AbstractAppender {
    private static final long serialVersionUID = 1L;
    // logEvents may be nulled to disable event tracking, this is useful in scenarios testing garbage collection.
    public List<LogEvent> logEvents = new CopyOnWriteArrayList<>();
    public CountDownLatch countDownLatch = null;

    public BlockingAppender(final String name) {
        super(name, null, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(final LogEvent event) {

        // for scenarios where domain objects log from their toString method in the background thread
        event.getMessage().getFormattedMessage();

        // may be a reusable event, make a copy, don't keep a reference to the original event
        List<LogEvent> events = logEvents;
        if (events != null) {
            events.add(Log4jLogEvent.createMemento(event));
        }

        if (countDownLatch == null) {
            return;
        }
        // block until the test class tells us to continue
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @PluginFactory
    public static BlockingAppender createAppender(
            @PluginAttribute
            @Required(message = "No name provided for HangingAppender")
            final String name,
            @PluginElement final Layout<? extends Serializable> layout,
            @PluginElement final Filter filter) {
        return new BlockingAppender(name);
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        super.stop(timeout, timeUnit, false);
        setStopped();
        return true;
    }
}
