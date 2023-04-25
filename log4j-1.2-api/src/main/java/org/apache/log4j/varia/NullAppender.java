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
package org.apache.log4j.varia;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A NullAppender never outputs a message to any device.
 */
public class NullAppender extends AppenderSkeleton {

    private static final NullAppender INSTANCE = new NullAppender();

    /**
     * Whenever you can, use this method to retreive an instance instead of instantiating a new one with <code>new</code>.
     */
    public static NullAppender getNullAppender() {
        return INSTANCE;
    }

    public NullAppender() {
        // noop
    }

    /**
     * There are no options to acticate.
     */
    @Override
    public void activateOptions() {
        // noop
    }

    /**
     * Does not do anything.
     */
    @Override
    protected void append(final LoggingEvent event) {
        // noop
    }

    @Override
    public void close() {
        // noop
    }

    /**
     * Does not do anything.
     */
    @Override
    public void doAppend(final LoggingEvent event) {
        // noop
    }

    /**
     * Whenever you can, use this method to retreive an instance instead of instantiating a new one with <code>new</code>.
     *
     * @deprecated Use getNullAppender instead. getInstance should have been static.
     */
    @Deprecated
    public NullAppender getInstance() {
        return INSTANCE;
    }

    /**
     * NullAppenders do not need a layout.
     */
    @Override
    public boolean requiresLayout() {
        return false;
    }
}
