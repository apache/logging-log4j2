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

package org.apache.logging.log4j.jul;

import java.util.logging.Logger;

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.MessageFormatMessageFactory;
import org.apache.logging.log4j.spi.LoggerContext;

/**
 * {@link Logger} registry implementation that uses log4j-core.
 *
 * @since 2.1
 */
public class CoreLoggerAdapter extends AbstractLoggerAdapter {

    private static final MessageFactory MESSAGE_FACTORY = new MessageFormatMessageFactory();

    @Override
    protected Logger newLogger(final String name, final LoggerContext context) {
        final org.apache.logging.log4j.spi.ExtendedLogger original = context.getLogger(name, MESSAGE_FACTORY);
        if (original instanceof org.apache.logging.log4j.core.Logger) {
            return new CoreLogger((org.apache.logging.log4j.core.Logger) original);
        }
        return new ApiLogger(original); // LOG4J2-1618 during shutdown, a SimpleLogger may be returned
    }
}
