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
package org.apache.logging.log4j.test;

import org.apache.logging.log4j.message.Message;

/**
 * A {@link Message} that cannot be serialized, although it implements {@link java.io.Serializable}.
 * <p>
 * One of its fields is not serializable, so serializing it throws {@link java.io.NotSerializableException}.
 * It can be used to test that serializable Log4j classes, which contain arbitrary messages or objects,
 * fall back to a serializable representation, like the formatted message.
 * </p>
 */
public final class UnserializableMessage implements Message {

    private static final long serialVersionUID = 1L;

    private final String text;

    @SuppressWarnings({"serial", "unused"})
    private final Object unserializableField = new Object();

    public UnserializableMessage(final String text) {
        this.text = text;
    }

    @Override
    public String getFormattedMessage() {
        return text;
    }

    @Override
    public String getFormat() {
        return text;
    }

    @Override
    public Object[] getParameters() {
        return null;
    }

    @Override
    public Throwable getThrowable() {
        return null;
    }

    @Override
    public String toString() {
        return text;
    }
}
