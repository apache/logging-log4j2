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

import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;

/**
 * Default implementation of LevelConverter strategy.
 *
 * @since 2.1
 */
public class DefaultLevelConverter implements LevelConverter {

    private final Map<java.util.logging.Level, Level> julToLog4j =
        new IdentityHashMap<java.util.logging.Level, Level>(9);
    private final Map<Level, java.util.logging.Level> log4jToJul =
        new IdentityHashMap<Level, java.util.logging.Level>(10);

    public DefaultLevelConverter() {
        // Map JUL to Log4j
        julToLog4j.put(java.util.logging.Level.OFF, Level.OFF);
        julToLog4j.put(java.util.logging.Level.FINEST, LevelTranslator.FINEST);
        julToLog4j.put(java.util.logging.Level.FINER, Level.TRACE);
        julToLog4j.put(java.util.logging.Level.FINE, Level.DEBUG);
        julToLog4j.put(java.util.logging.Level.CONFIG, LevelTranslator.CONFIG);
        julToLog4j.put(java.util.logging.Level.INFO, Level.INFO);
        julToLog4j.put(java.util.logging.Level.WARNING, Level.WARN);
        julToLog4j.put(java.util.logging.Level.SEVERE, Level.ERROR);
        julToLog4j.put(java.util.logging.Level.ALL, Level.ALL);
        // Map Log4j to JUL
        log4jToJul.put(Level.OFF, java.util.logging.Level.OFF);
        log4jToJul.put(LevelTranslator.FINEST, java.util.logging.Level.FINEST);
        log4jToJul.put(Level.TRACE, java.util.logging.Level.FINER);
        log4jToJul.put(Level.DEBUG, java.util.logging.Level.FINE);
        log4jToJul.put(LevelTranslator.CONFIG, java.util.logging.Level.CONFIG);
        log4jToJul.put(Level.INFO, java.util.logging.Level.INFO);
        log4jToJul.put(Level.WARN, java.util.logging.Level.WARNING);
        log4jToJul.put(Level.ERROR, java.util.logging.Level.SEVERE);
        log4jToJul.put(Level.FATAL, java.util.logging.Level.SEVERE);
        log4jToJul.put(Level.ALL, java.util.logging.Level.ALL);
    }

    @Override
    public Level toLevel(final java.util.logging.Level javaLevel) {
        return julToLog4j.get(javaLevel);
    }

    @Override
    public java.util.logging.Level toJavaLevel(final Level level) {
        return log4jToJul.get(level);
    }
}
