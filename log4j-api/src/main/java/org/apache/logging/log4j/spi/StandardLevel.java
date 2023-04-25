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

import java.util.EnumSet;

/**
 * Standard Logging Levels as an enumeration for use internally. This enum is used as a parameter in any public APIs.
 */
public enum StandardLevel {

    /**
     * No events will be logged.
     */
    OFF(0),

    /**
     * A severe error that will prevent the application from continuing.
     */
    FATAL(100),

    /**
     * An error in the application, possibly recoverable.
     */
    ERROR(200),

    /**
     * An event that might possible lead to an error.
     */
    WARN(300),

    /**
     * An event for informational purposes.
     */
    INFO(400),

    /**
     * A general debugging event.
     */
    DEBUG(500),

    /**
     * A fine-grained debug message, typically capturing the flow through the application.
     */
    TRACE(600),

    /**
     * All events should be logged.
     */
    ALL(Integer.MAX_VALUE);

    private static final EnumSet<StandardLevel> LEVELSET = EnumSet.allOf(StandardLevel.class);

    private final int intLevel;

    StandardLevel(final int val) {
        intLevel = val;
    }

    /**
     * Returns the integer value of the Level.
     *
     * @return the integer value of the Level.
     */
    public int intLevel() {
        return intLevel;
    }

    /**
     * Method to convert custom Levels into a StandardLevel for conversion to other systems.
     *
     * @param intLevel The integer value of the Level.
     * @return The StandardLevel.
     */
    public static StandardLevel getStandardLevel(final int intLevel) {
        StandardLevel level = StandardLevel.OFF;
        for (final StandardLevel lvl : LEVELSET) {
            if (lvl.intLevel() > intLevel) {
                break;
            }
            level = lvl;
        }
        return level;
    }
}
