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

    private final Map<java.util.logging.Level, Level> JDK_TO_LOG4J =
        new IdentityHashMap<java.util.logging.Level, Level>(9);
    private final Map<Level, java.util.logging.Level> LOG4J_TO_JDK =
        new IdentityHashMap<Level, java.util.logging.Level>(9);

    public DefaultLevelConverter() {
        JDK_TO_LOG4J.put(java.util.logging.Level.OFF, Level.OFF);
        JDK_TO_LOG4J.put(java.util.logging.Level.FINEST, LevelTranslator.FINEST);
        JDK_TO_LOG4J.put(java.util.logging.Level.FINER, Level.TRACE);
        JDK_TO_LOG4J.put(java.util.logging.Level.FINE, Level.DEBUG);
        JDK_TO_LOG4J.put(java.util.logging.Level.CONFIG, LevelTranslator.CONFIG);
        JDK_TO_LOG4J.put(java.util.logging.Level.INFO, Level.INFO);
        JDK_TO_LOG4J.put(java.util.logging.Level.WARNING, Level.WARN);
        JDK_TO_LOG4J.put(java.util.logging.Level.SEVERE, Level.ERROR);
        JDK_TO_LOG4J.put(java.util.logging.Level.ALL, Level.ALL);
        LOG4J_TO_JDK.put(Level.OFF, java.util.logging.Level.OFF);
        LOG4J_TO_JDK.put(LevelTranslator.FINEST, java.util.logging.Level.FINEST);
        LOG4J_TO_JDK.put(Level.TRACE, java.util.logging.Level.FINER);
        LOG4J_TO_JDK.put(Level.DEBUG, java.util.logging.Level.FINE);
        LOG4J_TO_JDK.put(LevelTranslator.CONFIG, java.util.logging.Level.CONFIG);
        LOG4J_TO_JDK.put(Level.INFO, java.util.logging.Level.INFO);
        LOG4J_TO_JDK.put(Level.WARN, java.util.logging.Level.WARNING);
        LOG4J_TO_JDK.put(Level.ERROR, java.util.logging.Level.SEVERE);
        LOG4J_TO_JDK.put(Level.ALL, java.util.logging.Level.ALL);
    }

    @Override
    public Level toLevel(final java.util.logging.Level javaLevel) {
        return JDK_TO_LOG4J.get(javaLevel);
    }

    @Override
    public java.util.logging.Level toJavaLevel(final Level level) {
        return LOG4J_TO_JDK.get(level);
    }
}
