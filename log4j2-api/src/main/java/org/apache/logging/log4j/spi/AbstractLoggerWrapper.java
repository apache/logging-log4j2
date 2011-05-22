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
package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

/**
 * Wrapper class that exposes the protected AbstractLogger methods to support wrapped loggers.
 */
public class AbstractLoggerWrapper extends AbstractLogger {

    protected final AbstractLogger logger;
    protected final String name;

    public AbstractLoggerWrapper(AbstractLogger logger, String name) {
        this.logger = logger;
        this.name = name;
    }

    @Override
    public void log(Marker marker, String fqcn, Level level, Message data, Throwable t) {
        logger.log(marker, fqcn, level, data, t);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String data) {
        return logger.isEnabled(level, marker, data);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String data, Throwable t) {
        return logger.isEnabled(level, marker, data, t);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String data, Object p1) {
        return logger.isEnabled(level, marker, data, p1);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String data, Object p1, Object p2) {
        return logger.isEnabled(level, marker, data, p1, p2);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String data, Object p1, Object p2, Object p3) {
        return logger.isEnabled(level, marker, data, p1, p2, p3);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, String data, Object p1, Object p2, Object p3,
                                Object... params) {
        return logger.isEnabled(level, marker, data, p2, p2, p3, params);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, Object data, Throwable t) {
        return logger.isEnabled(level, marker, data, t);
    }

    @Override
    public boolean isEnabled(Level level, Marker marker, Message data, Throwable t) {
        return logger.isEnabled(level, marker, data, t);
    }
}
