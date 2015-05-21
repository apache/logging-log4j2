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
package org.apache.logging.log4j.jcl;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.spi.LoggerAdapter;

/**
 * Log4j binding for Commons Logging.
 * {@inheritDoc}
 */
public class LogFactoryImpl extends LogFactory {

    private final LoggerAdapter<Log> adapter = new LogAdapter();

    private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();

    @Override
    public Log getInstance(final String name) throws LogConfigurationException {
        return adapter.getLogger(name);
    }

    @Override
    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    @Override
    public String[] getAttributeNames() {
        return attributes.keySet().toArray(new String[attributes.size()]);
    }

    @Override
    public Log getInstance(@SuppressWarnings("rawtypes") final Class clazz) throws LogConfigurationException {
        return getInstance(clazz.getName());
    }

    /**
     * This method is supposed to clear all loggers. In this implementation it will clear all the logger
     * wrappers but the loggers managed by the underlying logger context will not be.
     */
    @Override
    public void release() {
        try {
            adapter.close();
        } catch (final IOException ignored) {
        }
    }

    @Override
    public void removeAttribute(final String name) {
        attributes.remove(name);
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        if (value != null) {
            attributes.put(name, value);
        } else {
            removeAttribute(name);
        }
    }

}
