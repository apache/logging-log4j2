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
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.message.MessageFactory;

/**
 * Creates keys used in maps for use in LoggerContext implementations.
 *
 * @deprecated with no replacement - no longer used
 * @since 2.5
 */
@Deprecated
public class LoggerContextKey {

    public static String create(final String name) {
        return create(name, AbstractLogger.DEFAULT_MESSAGE_FACTORY_CLASS);
    }

    public static String create(final String name, final MessageFactory messageFactory) {
        final Class<? extends MessageFactory> messageFactoryClass =
                messageFactory != null ? messageFactory.getClass() : AbstractLogger.DEFAULT_MESSAGE_FACTORY_CLASS;
        return create(name, messageFactoryClass);
    }

    public static String create(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        final Class<? extends MessageFactory> mfClass =
                messageFactoryClass != null ? messageFactoryClass : AbstractLogger.DEFAULT_MESSAGE_FACTORY_CLASS;
        return name + "." + mfClass.getName();
    }
}
