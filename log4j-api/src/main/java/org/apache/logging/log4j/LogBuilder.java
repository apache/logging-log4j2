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
package org.apache.logging.log4j;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Supplier;


/**
 * Interface for constructing log events before logging them. Instances of LogBuilders should only be created
 * by calling one of the Logger methods that return a LogBuilder.
 */
public interface LogBuilder {

    public static final LogBuilder NOOP = new LogBuilder() {};

    default LogBuilder withMarker(Marker marker) {
        return this;
    }

    default LogBuilder withThrowable(Throwable throwable) {
        return this;
    }

    default LogBuilder withLocation() {
        return this;
    }

    default LogBuilder withLocation(StackTraceElement location) {
        return this;
    }

    default void log(CharSequence message) {
    }

    default void log(String message) {
    }

    default void log(String message, Object... params) {
    }

    default void log(String message, Supplier<?>... params) {
    }

    default void log(Message message) {
    }

    default void log(Supplier<Message> messageSupplier) {
    }

    default void log(Object message) {

    }
}
