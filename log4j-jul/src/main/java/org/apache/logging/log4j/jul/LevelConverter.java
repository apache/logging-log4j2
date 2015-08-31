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

import org.apache.logging.log4j.Level;

/**
 * Strategy interface to convert between custom Log4j {@link Level Levels} and JUL
 * {@link java.util.logging.Level Levels}.
 *
 * @see Constants#LEVEL_CONVERTER_PROPERTY
 * @since 2.1
 */
public interface LevelConverter {

    /**
     * Converts a JDK logging Level to a Log4j logging Level.
     *
     * @param javaLevel JDK Level to convert, may be null per the JUL specification.
     * @return converted Level or {@code null} if the given level could not be converted.
     */
    Level toLevel(java.util.logging.Level javaLevel);

    /**
     * Converts a Log4j logging Level to a JDK logging Level.
     *
     * @param level Log4j Level to convert.
     * @return converted Level or {@code null} if the given level could not be converted.
     */
    java.util.logging.Level toJavaLevel(Level level);
}
