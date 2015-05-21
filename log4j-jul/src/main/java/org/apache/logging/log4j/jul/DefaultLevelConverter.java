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

    private final Map<java.util.logging.Level, Level> julToLog4j = new IdentityHashMap<>(9);
    private final Map<Level, java.util.logging.Level> log4jToJul = new IdentityHashMap<>(10);

    public DefaultLevelConverter() {
        // Map JUL to Log4j
        mapJulToLog4j(java.util.logging.Level.OFF, Level.OFF);
        mapJulToLog4j(java.util.logging.Level.FINEST, LevelTranslator.FINEST);
        mapJulToLog4j(java.util.logging.Level.FINER, Level.TRACE);
        mapJulToLog4j(java.util.logging.Level.FINE, Level.DEBUG);
        mapJulToLog4j(java.util.logging.Level.CONFIG, LevelTranslator.CONFIG);
        mapJulToLog4j(java.util.logging.Level.INFO, Level.INFO);
        mapJulToLog4j(java.util.logging.Level.WARNING, Level.WARN);
        mapJulToLog4j(java.util.logging.Level.SEVERE, Level.ERROR);
        mapJulToLog4j(java.util.logging.Level.ALL, Level.ALL);
        // Map Log4j to JUL
        mapLog4jToJul(Level.OFF, java.util.logging.Level.OFF);
        mapLog4jToJul(LevelTranslator.FINEST, java.util.logging.Level.FINEST);
        mapLog4jToJul(Level.TRACE, java.util.logging.Level.FINER);
        mapLog4jToJul(Level.DEBUG, java.util.logging.Level.FINE);
        mapLog4jToJul(LevelTranslator.CONFIG, java.util.logging.Level.CONFIG);
        mapLog4jToJul(Level.INFO, java.util.logging.Level.INFO);
        mapLog4jToJul(Level.WARN, java.util.logging.Level.WARNING);
        mapLog4jToJul(Level.ERROR, java.util.logging.Level.SEVERE);
        mapLog4jToJul(Level.FATAL, java.util.logging.Level.SEVERE);
        mapLog4jToJul(Level.ALL, java.util.logging.Level.ALL);
    }
    
    /*
     * TODO consider making public.
     */
    private void mapJulToLog4j(java.util.logging.Level julLevel, Level level) {
        julToLog4j.put(julLevel, level);
    }
    
    /*
     * TODO consider making public.
     */
    private void mapLog4jToJul(Level level, java.util.logging.Level julLevel) {
        log4jToJul.put(level, julLevel);
    }

    @Override
    public java.util.logging.Level toJavaLevel(final Level level) {
        return log4jToJul.get(level);
    }

    @Override
    public Level toLevel(final java.util.logging.Level javaLevel) {
        return julToLog4j.get(javaLevel);
    }
}
