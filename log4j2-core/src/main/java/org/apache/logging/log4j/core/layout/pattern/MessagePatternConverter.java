/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

package org.apache.logging.log4j.core.layout.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.Message;


/**
 * Return the event's rendered message in a StringBuffer.
 */
@Plugin(name="MessagePatternConverter", type="Converter")
@ConverterKeys({"m", "message"})
public final class MessagePatternConverter extends LogEventPatternConverter {

    private final String format;

    /**
     * Private constructor.
     * @param options options, may be null.
     */
    private MessagePatternConverter(final String[] options) {
        super("Message", "message");
        format = (options != null && options.length > 0) ? options[0] : null;
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options options, may be null.
     * @return instance of pattern converter.
     */
    public static MessagePatternConverter newInstance(final String[] options) {
        return new MessagePatternConverter(options);
    }

    /**
     * {@inheritDoc}
     */
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        Message msg = event.getMessage();
        if (msg != null && msg instanceof FormattedMessage) {
            ((FormattedMessage) msg).setFormat(format);
        }
        toAppendTo.append(msg.getFormattedMessage());
    }
}
