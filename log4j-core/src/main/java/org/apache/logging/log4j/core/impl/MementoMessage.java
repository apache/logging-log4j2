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
package org.apache.logging.log4j.core.impl;

import java.util.Arrays;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * <em>Consider this class private.</em>
 *
 * {@link MementoMessage} is intended to be used when we need to make an
 * immutable copy of a {@link Message} without forgetting the original
 * {@link Message#getFormat()} and {@link Message#getParameters()} values.
 *
 * @since 3.0
 */
public final class MementoMessage implements Message, StringBuilderFormattable {

    private final String formattedMessage;
    private final String format;
    private final Object[] parameters;

    public MementoMessage(final String formattedMessage, final String format, final Object[] parameters) {
        this.formattedMessage = formattedMessage;
        this.format = format;
        this.parameters = parameters;
    }

    @Override
    public String getFormattedMessage() {
        return formattedMessage;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    /**
     * Always returns null.
     *
     * @return null
     */
    @Override
    public Throwable getThrowable() {
        return null;
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        buffer.append(formattedMessage);
    }

    @Override
    public String toString() {
        return "MementoMessage{" + "formattedMessage='"
                + formattedMessage + '\'' + ", format='"
                + format + '\'' + ", parameters="
                + Arrays.toString(parameters) + '}';
    }
}
