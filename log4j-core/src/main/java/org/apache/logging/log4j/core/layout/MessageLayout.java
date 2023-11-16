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
package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;

/**
 * Formats a {@link LogEvent} in its {@link Message} form.
 * <p>
 * Useful in combination with a JMS Appender to map a Log4j {@link org.apache.logging.log4j.message.MapMessage} or
 * {@link org.apache.logging.log4j.message.StringMapMessage} to a JMS {@link javax.jms.MapMessage}.
 * </p>
 */
@Plugin(name = "MessageLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class MessageLayout extends AbstractLayout<Message> {

    public MessageLayout() {
        super(null, null, null);
    }

    public MessageLayout(final Configuration configuration, final byte[] header, final byte[] footer) {
        super(configuration, header, footer);
    }

    @Override
    public byte[] toByteArray(final LogEvent event) {
        return null;
    }

    @Override
    public Message toSerializable(final LogEvent event) {
        return event.getMessage();
    }

    @Override
    public String getContentType() {
        return null;
    }

    @PluginFactory
    public static Layout<?> createLayout() {
        return new MessageLayout();
    }
}
