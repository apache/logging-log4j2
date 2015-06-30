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
package org.apache.log4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Locale;

import org.apache.logging.log4j.util.Strings;

/**
 * Defines the minimum set of levels recognized by the system, that is
 * <code>OFF</code>, <code>FATAL</code>, <code>ERROR</code>,
 * <code>WARN</code>, <code>INFO</code>, <code>DEBUG</code>
 * and <code>ALL</code>.
 * <p>
 * The <code>Level</code> class may be subclassed to define a larger
 * level set.
 * </p>
 */
public class Level extends Priority implements Serializable {

    /**
     * TRACE level integer value.
     *
     * @since 1.2.12
     */
    public static final int TRACE_INT = 5000;

    /**
     * The <code>OFF</code> has the highest possible rank and is
     * intended to turn off logging.
     */
    public static final Level OFF = new Level(OFF_INT, "OFF", 0);

    /**
     * The <code>FATAL</code> level designates very severe error
     * events that will presumably lead the application to abort.
     */
    public static final Level FATAL = new Level(FATAL_INT, "FATAL", 0);

    /**
     * The <code>ERROR</code> level designates error events that
     * might still allow the application to continue running.
     */
    public static final Level ERROR = new Level(ERROR_INT, "ERROR", 3);

    /**
     * The <code>WARN</code> level designates potentially harmful situations.
     */
    public static final Level WARN = new Level(WARN_INT, "WARN", 4);

    /**
     * The <code>INFO</code> level designates informational messages
     * that highlight the progress of the application at coarse-grained
     * level.
     */
    public static final Level INFO = new Level(INFO_INT, "INFO", 6);

    /**
     * The <code>DEBUG</code> Level designates fine-grained
     * informational events that are most useful to debug an
     * application.
     */
    public static final Level DEBUG = new Level(DEBUG_INT, "DEBUG", 7);

    /**
     * The <code>TRACE</code> Level designates finer-grained
     * informational events than the <code>DEBUG</code> level.
     */
    public static final Level TRACE = new Level(TRACE_INT, "TRACE", 7);

    /**
     * The <code>ALL</code> has the lowest possible rank and is intended to
     * turn on all logging.
     */
    public static final Level ALL = new Level(ALL_INT, "ALL", 7);

    /**
     * Serialization version id.
     */
    private static final long serialVersionUID = 3491141966387921974L;

    /**
     * Instantiate a Level object.
     *
     * @param level            The logging level.
     * @param levelStr         The level name.
     * @param syslogEquivalent The matching syslog level.
     */
    protected Level(final int level, final String levelStr, final int syslogEquivalent) {
        super(level, levelStr, syslogEquivalent);
    }


    /**
     * Convert the string passed as argument to a level. If the
     * conversion fails, then this method returns {@link #DEBUG}.
     *
     * @param sArg The level name.
     * @return The Level.
     */
    public static Level toLevel(final String sArg) {
        return toLevel(sArg, Level.DEBUG);
    }

    /**
     * Convert an integer passed as argument to a level. If the
     * conversion fails, then this method returns {@link #DEBUG}.
     *
     * @param val The integer value of the Level.
     * @return The Level.
     */
    public static Level toLevel(final int val) {
        return toLevel(val, Level.DEBUG);
    }

    /**
     * Convert an integer passed as argument to a level. If the
     * conversion fails, then this method returns the specified default.
     *
     * @param val          The integer value of the Level.
     * @param defaultLevel the default level if the integer doesn't match.
     * @return The matching Level.
     */
    public static Level toLevel(final int val, final Level defaultLevel) {
        switch (val) {
            case ALL_INT:
                return ALL;
            case DEBUG_INT:
                return Level.DEBUG;
            case INFO_INT:
                return Level.INFO;
            case WARN_INT:
                return Level.WARN;
            case ERROR_INT:
                return Level.ERROR;
            case FATAL_INT:
                return Level.FATAL;
            case OFF_INT:
                return OFF;
            case TRACE_INT:
                return Level.TRACE;
            default:
                return defaultLevel;
        }
    }

    /**
     * Convert the string passed as argument to a level. If the
     * conversion fails, then this method returns the value of
     * <code>defaultLevel</code>.
     * @param sArg The name of the Level.
     * @param defaultLevel The default Level to use.
     * @return the matching Level.
     */
    public static Level toLevel(final String sArg, final Level defaultLevel) {
        if (sArg == null) {
            return defaultLevel;
        }
        final String s = sArg.toUpperCase(Locale.ROOT);
        switch (s) {
        case "ALL":
            return Level.ALL;
        case "DEBUG":
            return Level.DEBUG;
        case "INFO":
            return Level.INFO;
        case "WARN":
            return Level.WARN;
        case "ERROR":
            return Level.ERROR;
        case "FATAL":
            return Level.FATAL;
        case "OFF":
            return Level.OFF;
        case "TRACE":
            return Level.TRACE;
        default:
            return defaultLevel;
        }
    }

    /**
     * Custom deserialization of Level.
     *
     * @param s serialization stream.
     * @throws IOException            if IO exception.
     * @throws ClassNotFoundException if class not found.
     */
    private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        level = s.readInt();
        syslogEquivalent = s.readInt();
        levelStr = s.readUTF();
        if (levelStr == null) {
            levelStr = Strings.EMPTY;
        }
    }

    /**
     * Serialize level.
     *
     * @param s serialization stream.
     * @throws IOException if exception during serialization.
     */
    private void writeObject(final ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(level);
        s.writeInt(syslogEquivalent);
        s.writeUTF(levelStr);
    }

    /**
     * Resolved deserialized level to one of the stock instances.
     * May be overridden in classes derived from Level.
     *
     * @return resolved object.
     * @throws ObjectStreamException if exception during resolution.
     */
    protected Object readResolve() throws ObjectStreamException {
        //
        //  if the deserialized object is exactly an instance of Level
        //
        if (getClass() == Level.class) {
            return toLevel(level);
        }
        //
        //   extension of Level can't substitute stock item
        //
        return this;
    }

}

