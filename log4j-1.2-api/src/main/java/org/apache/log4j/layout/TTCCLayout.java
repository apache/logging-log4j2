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
package org.apache.log4j.layout;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Port of TTCCLayout in Log4j 1.x. Provided for compatibility with existing Log4j 1 configurations.
 *
 * Originally developed by Ceki G&uuml;lc&uuml;, Heinz Richter, Christopher Williams, Mathias Bogaert.
 */
@Plugin(name = "TTCCLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public final class TTCCLayout extends AbstractStringLayout {

    private final boolean threadPrinting;
    private final boolean categoryPrefixing;
    private final boolean contextPrinting;

    final long startTime = System.currentTimeMillis();

    @PluginFactory
    public static TTCCLayout createLayout(
            // @formatter:off
            @PluginAttribute(value = "threadPrinting", defaultBoolean = true) final boolean threadPrinting,
            @PluginAttribute(value = "categoryPrefixing", defaultBoolean = true) final boolean categoryPrefixing,
            @PluginAttribute(value = "contextPrinting", defaultBoolean = true) final boolean contextPrinting
            // @formatter:on
    ) {
        return new TTCCLayout(threadPrinting, categoryPrefixing, contextPrinting);
    }

    private TTCCLayout(boolean threadPrinting, boolean categoryPrefixing, boolean contextPrinting) {
        super(StandardCharsets.UTF_8);
        this.threadPrinting = threadPrinting;
        this.categoryPrefixing = categoryPrefixing;
        this.contextPrinting = contextPrinting;
    }

    public boolean isThreadPrinting() {
        return threadPrinting;
    }

    public boolean isCategoryPrefixing() {
        return categoryPrefixing;
    }

    public boolean isContextPrinting() {
        return contextPrinting;
    }

    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        final StringBuilder text = getStringBuilder();
        formatTo(event, text);
        getStringBuilderEncoder().encode(text, destination);
    }

    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder text = getStringBuilder();
        formatTo(event, text);
        return text.toString();
    }

    private void formatTo(final LogEvent event, final StringBuilder buf) {
        buf.append(event.getTimeMillis() - startTime);
        buf.append(' ');

        if (threadPrinting) {
            buf.append('[');
            buf.append(event.getThreadName());
            buf.append("] ");
        }

        buf.append(event.getLevel().toString());
        buf.append(' ');

        if (categoryPrefixing) {
            buf.append(event.getLoggerName());
            buf.append(' ');
        }

        if (this.contextPrinting) {
            List<String> ndc = event.getContextStack().asList();
            if (!ndc.isEmpty()) {
                for (String ndcElement : ndc) {
                    buf.append(ndcElement);
                    buf.append(' ');
                }
            }
        }

        buf.append("- ");
        final Message message = event.getMessage();
        if (message instanceof StringBuilderFormattable) {
            ((StringBuilderFormattable)message).formatTo(buf);
        } else {
            buf.append(message.getFormattedMessage());
        }

        buf.append(Constants.LINE_SEPARATOR);
    }

}
