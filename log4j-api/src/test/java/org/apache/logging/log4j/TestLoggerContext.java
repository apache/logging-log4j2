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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 *
 */
public class TestLoggerContext implements LoggerContext {
    private final Map<String, ExtendedLogger> map = new HashMap<>();

    @Override
    public ExtendedLogger getLogger(final String name) {
        final ExtendedLogger extendedLogger = map.get(name);
        if (extendedLogger != null) {
			return extendedLogger;
        }
        final ExtendedLogger logger = new TestLogger(name);
        map.put(name, logger);
        return logger;
    }

    @Override
    public ExtendedLogger getLogger(final String name, final MessageFactory messageFactory) {
        return new TestLogger(name, messageFactory);
    }

    @Override
    public Object getExternalContext() {
        return null;
    }

    @Override
    public boolean hasLogger(final String name) {
        return false;
    }

    @Override
    public boolean hasLogger(final String name, final MessageFactory messageFactory) {
        return false;
    }

    @Override
    public boolean hasLogger(final String name, final Class<? extends MessageFactory> messageFactoryClass) {
        return false;
    }

}
