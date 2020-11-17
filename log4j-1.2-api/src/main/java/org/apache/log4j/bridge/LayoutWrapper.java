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
import org.apache.log4j.spi.LoggingEvent;

/**
 * Bridge between the Log4j 1 Layout and a Log4j 2 Layout.
 */
public class LayoutWrapper extends Layout {

    private final org.apache.logging.log4j.core.Layout<?> layout;

    public LayoutWrapper(org.apache.logging.log4j.core.Layout<?> layout) {
        this.layout = layout;
    }

    @Override
    public String format(LoggingEvent event) {
        return layout.toSerializable(((LogEventAdapter)event).getEvent()).toString();
    }

    @Override
    public boolean ignoresThrowable() {
        return false;
    }

    public org.apache.logging.log4j.core.Layout<?> getLayout() {
        return this.layout;
    }
}
