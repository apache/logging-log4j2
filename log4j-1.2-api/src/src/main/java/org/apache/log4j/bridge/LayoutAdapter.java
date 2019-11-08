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
package org.apache.log4j.bridge;

import org.apache.log4j.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;

import java.util.HashMap;
import java.util.Map;

/**
 * Class Description goes here.
 */
public class LayoutAdapter implements org.apache.logging.log4j.core.Layout<String> {
    private Layout layout;

    public LayoutAdapter(Layout layout) {
        this.layout = layout;
    }


    @Override
    public byte[] getFooter() {
        return layout.getFooter() == null ? null : layout.getFooter().getBytes();
    }

    @Override
    public byte[] getHeader() {
        return layout.getHeader() == null ? null : layout.getHeader().getBytes();
    }

    @Override
    public byte[] toByteArray(LogEvent event) {
        String result = layout.format(new LogEventAdapter(event));
        return result == null ? null : result.getBytes();
    }

    @Override
    public String toSerializable(LogEvent event) {
        return layout.format(new LogEventAdapter(event));
    }

    @Override
    public String getContentType() {
        return layout.getContentType();
    }

    @Override
    public Map<String, String> getContentFormat() {
        return new HashMap<>();
    }

    @Override
    public void encode(LogEvent event, ByteBufferDestination destination) {
        final byte[] data = toByteArray(event);
        destination.writeBytes(data, 0, data.length);
    }
}
