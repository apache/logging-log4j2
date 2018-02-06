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

/**
 * Represents a Message that is snapshot of another Message at some point of time.
 */
public class MementoMessage implements Message {

    private final String format;
    private final Object[] parameters;
    private final String formattedMessage;
    private final Throwable throwable;

    public MementoMessage(String format, Object[] parameters, String formattedMessage, Throwable throwable) {
        this.format = format;
        this.parameters = parameters;
        this.formattedMessage = formattedMessage;
        this.throwable = throwable;
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

    @Override
    public Throwable getThrowable() {
        return throwable;
    }
}
