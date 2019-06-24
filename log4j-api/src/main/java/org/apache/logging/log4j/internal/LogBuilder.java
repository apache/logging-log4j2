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
package org.apache.logging.log4j.internal;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

import java.util.function.Supplier;

/**
 * Interface for constructing log events before logging them.
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

    default LogBuilder withMessage(Message msg) {
        return this;
    }

    default LogBuilder withMessage(String msg) {
        return this;
    }

    default LogBuilder withMessage(Object msg) {
        return this;
    }

    default LogBuilder withParameters(Object... params) {
        return this;
    }

    default LogBuilder withParameters(Supplier<Object>... params) {
        return this;
    }

    default void log() {
    }
}
