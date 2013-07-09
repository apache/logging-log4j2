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

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.LoggerContext;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TestLoggerContext implements LoggerContext {
    private final Map<String, Logger> map = new HashMap<String, Logger>();

    @Override
    public Logger getLogger(final String name) {
        if (map.containsKey(name)) {
            return map.get(name);
        }
        final Logger logger = new TestLogger(name);
        map.put(name, logger);
        return logger;
    }

    @Override
    public Logger getLogger(final String name, final MessageFactory messageFactory) {
        return new TestLogger(name, messageFactory);
    }

    @Override
    public boolean hasLogger(final String name) {
        return false;
    }

    @Override
    public Object getExternalContext() {
        return null;
    }

}
