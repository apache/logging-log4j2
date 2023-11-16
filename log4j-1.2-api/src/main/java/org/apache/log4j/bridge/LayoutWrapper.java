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
package org.apache.log4j.bridge;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Bridge between the Log4j 1 Layout and a Log4j 2 Layout.
 */
public class LayoutWrapper extends Layout {

    private final org.apache.logging.log4j.core.Layout<?> layout;

    /**
     * Adapts a Log4j 2.x layout into a Log4j 1.x layout. Applying this method to
     * the result of {@link LayoutAdapter#adapt(Layout)} should return the original
     * Log4j 1.x layout.
     *
     * @param layout a Log4j 2.x layout
     * @return a Log4j 1.x layout or {@code null} if the parameter is {@code null}
     */
    public static Layout adapt(final org.apache.logging.log4j.core.Layout<?> layout) {
        if (layout instanceof LayoutAdapter) {
            return ((LayoutAdapter) layout).getLayout();
        }
        if (layout != null) {
            return new LayoutWrapper(layout);
        }
        return null;
    }

    /**
     * Constructs a new instance.
     *
     * @param layout The layout to wrap.
     */
    public LayoutWrapper(final org.apache.logging.log4j.core.Layout<?> layout) {
        this.layout = layout;
    }

    @Override
    public String format(final LoggingEvent event) {
        return layout.toSerializable(((LogEventAdapter) event).getEvent()).toString();
    }

    /**
     * Unwraps.
     *
     * @return The wrapped object.
     */
    public org.apache.logging.log4j.core.Layout<?> getLayout() {
        return this.layout;
    }

    @Override
    public boolean ignoresThrowable() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("LayoutWrapper [layout=%s]", layout);
    }
}
