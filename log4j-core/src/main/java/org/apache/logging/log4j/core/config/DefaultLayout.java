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
package org.apache.logging.log4j.core.config;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.status.StatusData;

/**
 * A simple layout used only by {@link DefaultConfiguration}
 * <p>
 *   This layout allows to create applications that don't contain {@link org.apache.logging.log4j.core.layout.PatternLayout}
 *   and all its patterns, e.g. GraalVM applications.
 * </p>
 *
 * @since 2.25.0
 */
final class DefaultLayout implements StringLayout {

    static final StringLayout INSTANCE = new DefaultLayout();

    private DefaultLayout() {}

    @Override
    public String toSerializable(LogEvent event) {
        return new StatusData(
                        event.getSource(),
                        event.getLevel(),
                        event.getMessage(),
                        event.getThrown(),
                        event.getThreadName())
                .getFormattedStatus();
    }

    @Override
    public byte[] toByteArray(LogEvent event) {
        return toSerializable(event).getBytes(Charset.defaultCharset());
    }

    @Override
    public void encode(LogEvent event, ByteBufferDestination destination) {
        final byte[] data = toByteArray(event);
        destination.writeBytes(data, 0, data.length);
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }

    @Override
    public Charset getCharset() {
        return Charset.defaultCharset();
    }

    @Override
    public byte[] getFooter() {
        return null;
    }

    @Override
    public byte[] getHeader() {
        return null;
    }

    @Override
    public Map<String, String> getContentFormat() {
        return Collections.emptyMap();
    }
}
