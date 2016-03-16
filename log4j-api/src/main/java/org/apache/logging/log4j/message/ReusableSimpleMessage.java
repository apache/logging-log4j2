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
package org.apache.logging.log4j.message;

import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Mutable Message wrapper around a String message.
 * @since 2.6
 */
@PerformanceSensitive("allocation")
public class ReusableSimpleMessage implements ReusableMessage {
    private static final long serialVersionUID = -9199974506498249809L;
    private static Object[] EMPTY_PARAMS = new Object[0];
    private String message;

    public void set(String message) {
        this.message = message;
    }

    @Override
    public String getFormattedMessage() {
        return message;
    }

    @Override
    public String getFormat() {
        return message;
    }

    @Override
    public Object[] getParameters() {
        return EMPTY_PARAMS;
    }

    @Override
    public Throwable getThrowable() {
        return null;
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        buffer.append(message);
    }
}

