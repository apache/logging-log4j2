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
package org.apache.logging.log4j.status;

import java.io.PrintStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.util.Strings;

/**
 * {@link org.apache.logging.log4j.simple.SimpleLogger} factory to be used by {@link StatusLogger} and {@link StatusConsoleListener}.
 */
final class SimpleLoggerFactory {

    private static final SimpleLoggerFactory INSTANCE = new SimpleLoggerFactory();

    private SimpleLoggerFactory() {}

    static SimpleLoggerFactory getInstance() {
        return INSTANCE;
    }

    SimpleLogger createSimpleLogger(
            final String name, final Level level, final MessageFactory messageFactory, final PrintStream stream) {
        final String dateFormat = StatusLogger.PROPS.getStringProperty(StatusLogger.STATUS_DATE_FORMAT);
        final boolean dateFormatProvided = Strings.isNotBlank(dateFormat);
        return new SimpleLogger(
                name,
                level,
                false,
                true,
                dateFormatProvided,
                false,
                dateFormat,
                messageFactory,
                StatusLogger.PROPS,
                stream);
    }
}
