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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.io.Serializable;

@Plugin(name = "Hanging", category = "Core", elementType = "appender", printObject = true)
public class HangingAppender extends AbstractAppender {

    private static final long serialVersionUID = 1L;

    private final long delay;

    public HangingAppender(final String name, final long delay) {
        super(name, null, null);
        this.delay = delay;
    }

    @Override
    public void append(final LogEvent event) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignore) {
            // ignore
        }
    }

    @PluginFactory
    public static HangingAppender createAppender(
            @PluginAttribute("name")
            @Required(message = "No name provided for HangingAppender")
            final String name,
            @PluginAttribute("delay") final long delay,
            @PluginElement("Layout") final Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter) {
        return new HangingAppender(name, delay);
    }

}
