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
package org.apache.log4j.helpers;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.Configurator;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * A convenience class to convert property values to specific types.
 */
public class OptionConverter {

    private static class CharMap {
        final char key;
        final char replacement;

        public CharMap(final char key, final char replacement) {
            this.key = key;
            this.replacement = replacement;
        }
    }

    static String DELIM_START = "${";
    static char DELIM_STOP = '}';
    static int DELIM_START_LEN = 2;
    static int DELIM_STOP_LEN = 1;
    private static final Logger LOGGER = StatusLogger.getLogger();
    /**
     * A Log4j 1.x level above or equal to this value is considered as OFF.
     */
    static final int MAX_CUTOFF_LEVEL =
            Priority.FATAL_INT + 100 * (StandardLevel.FATAL.intLevel() - StandardLevel.OFF.intLevel() - 1) + 1;
    /**
     * A Log4j 1.x level below or equal to this value is considered as ALL.
     *
     * Log4j 2.x ALL to TRACE interval is shorter. This is {@link Priority#ALL_INT}
     * plus the difference.
     */
    static final int MIN_CUTOFF_LEVEL = Priority.ALL_INT
            + Level.TRACE_INT
            - (Priority.ALL_INT + StandardLevel.ALL.intLevel())
            + StandardLevel.TRACE.intLevel();
    /**
     * Cache of currently known levels.
     */
    static final ConcurrentMap<String, Level> LEVELS = new ConcurrentHashMap<>();
    /**
     * Postfix for all Log4j 2.x level names.
     */
    private static final String LOG4J2_LEVEL_CLASS = org.apache.logging.log4j.Level.class.getName();

    private static final CharMap[] charMap = new CharMap[] {
        new CharMap('n', '\n'),
        new CharMap('r', '\r'),
        new CharMap('t', '\t'),
        new CharMap('f', '\f'),
        new CharMap('\b', '\b'),
        new CharMap('\"', '\"'),
        new CharMap('\'', '\''),
        new CharMap('\\', '\\')
    };

    public static String[] concatanateArrays(final String[] l, final String[] r) {
        final int len = l.length + r.length;
        final String[] a = new String[len];

        System.arraycopy(l, 0, a, 0, l.length);
        System.arraycopy(r, 0, a, l.length, r.length);

        return a;
    }

    static int toLog4j2Level(final int v1Level) {
        // I don't believe anyone uses values much bigger than FATAL
        if (v1Level >= MAX_CUTOFF_LEVEL) {
            return StandardLevel.OFF.intLevel();
        }
        // Linear transformation up to debug: CUTOFF_LEVEL -> OFF, DEBUG -> DEBUG
        if (v1Level > Priority.DEBUG_INT) {
            final int offset = Math.round((v1Level - Priority.DEBUG_INT) / 100.0f);
            return StandardLevel.DEBUG.intLevel() - offset;
        }
        // Steeper linear transformation
        if (v1Level > Level.TRACE_INT) {
            final int offset = Math.round((v1Level - Level.TRACE_INT) / 50.0f);
            return StandardLevel.TRACE.intLevel() - offset;
        }
        if (v1Level > MIN_CUTOFF_LEVEL) {
            final int offset = Level.TRACE_INT - v1Level;
            return StandardLevel.TRACE.intLevel() + offset;
        }
        return StandardLevel.ALL.intLevel();
    }

    static int toLog4j1Level(final int v2Level) {
        if (v2Level == StandardLevel.ALL.intLevel()) {
            return Priority.ALL_INT;
        }
        if (v2Level > StandardLevel.TRACE.intLevel()) {
            return MIN_CUTOFF_LEVEL + (StandardLevel.ALL.intLevel() - v2Level);
        }
        // Inflating by 50
        if (v2Level > StandardLevel.DEBUG.intLevel()) {
            return Level.TRACE_INT + 50 * (StandardLevel.TRACE.intLevel() - v2Level);
        }
        // Inflating by 100
        if (v2Level > StandardLevel.OFF.intLevel()) {
            return Priority.DEBUG_INT + 100 * (StandardLevel.DEBUG.intLevel() - v2Level);
        }
        return Priority.OFF_INT;
    }

    static int toSyslogLevel(final int v2Level) {
        if (v2Level <= StandardLevel.FATAL.intLevel()) {
            return 0;
        }
        if (v2Level <= StandardLevel.ERROR.intLevel()) {
            return 3
                    - (3 * (StandardLevel.ERROR.intLevel() - v2Level))
                            / (StandardLevel.ERROR.intLevel() - StandardLevel.FATAL.intLevel());
        }
        if (v2Level <= StandardLevel.WARN.intLevel()) {
            return 4;
        }
        if (v2Level <= StandardLevel.INFO.intLevel()) {
            return 6
                    - (2 * (StandardLevel.INFO.intLevel() - v2Level))
                            / (StandardLevel.INFO.intLevel() - StandardLevel.WARN.intLevel());
        }
        return 7;
    }

    public static org.apache.logging.log4j.Level createLevel(final Priority level) {
        final String name =
                toRootUpperCase(level.toString()) + "#" + level.getClass().getName();
        return org.apache.logging.log4j.Level.forName(name, toLog4j2Level(level.toInt()));
    }

    public static org.apache.logging.log4j.Level convertLevel(final Priority level) {
        return level != null ? level.getVersion2Level() : org.apache.logging.log4j.Level.ERROR;
    }

    /**
     * @param level
     * @return
     */
    public static Level convertLevel(final org.apache.logging.log4j.Level level) {
        // level is standard or was created by Log4j 1.x custom level
        Level actualLevel = toLevel(level.name(), null);
        // level was created by Log4j 2.x
        if (actualLevel == null) {
            actualLevel = toLevel(LOG4J2_LEVEL_CLASS, level.name(), null);
        }
        return actualLevel != null ? actualLevel : Level.ERROR;
    }

    public static org.apache.logging.log4j.Level convertLevel(
            final String level, final org.apache.logging.log4j.Level defaultLevel) {
        final Level actualLevel = toLevel(level, null);
        return actualLevel != null ? actualLevel.getVersion2Level() : defaultLevel;
    }

    public static String convertSpecialChars(final String s) {
        char c;
        final int len = s.length();
        final StringBuilder sbuf = new StringBuilder(len);

        int i = 0;
        while (i < len) {
            c = s.charAt(i++);
            if (c == '\\') {
                c = s.charAt(i++);
                for (final CharMap entry : charMap) {
                    if (entry.key == c) {
                        c = entry.replacement;
                    }
                }
            }
            sbuf.append(c);
        }
        return sbuf.toString();
    }

    /**
     * Find the value corresponding to <code>key</code> in
     * <code>props</code>. Then perform variable substitution on the
     * found value.
     * @param key The key used to locate the substitution string.
     * @param props The properties to use in the substitution.
     * @return The substituted string.
     */
    public static String findAndSubst(final String key, final Properties props) {
        final String value = props.getProperty(key);
        if (value == null) {
            return null;
        }

        try {
            return substVars(value, props);
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Bad option value [{}].", value, e);
            return value;
        }
    }

    /**
     * Very similar to <code>System.getProperty</code> except
     * that the {@link SecurityException} is hidden.
     *
     * @param key The key to search for.
     * @param def The default value to return.
     * @return the string value of the system property, or the default
     * value if there is no property with that key.
     * @since 1.1
     */
    public static String getSystemProperty(final String key, final String def) {
        try {
            return System.getProperty(key, def);
        } catch (final Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
            LOGGER.debug("Was not allowed to read system property \"{}\".", key);
            return def;
        }
    }

    /**
     * Instantiate an object given a class name. Check that the
     * <code>className</code> is a subclass of
     * <code>superClass</code>. If that test fails or the object could
     * not be instantiated, then <code>defaultValue</code> is returned.
     *
     * @param className    The fully qualified class name of the object to instantiate.
     * @param superClass   The class to which the new object should belong.
     * @param defaultValue The object to return in case of non-fulfillment
     * @return The created object.
     */
    public static Object instantiateByClassName(
            final String className, final Class<?> superClass, final Object defaultValue) {
        if (className != null) {
            try {
                final Object obj = LoaderUtil.newInstanceOf(className);
                if (!superClass.isAssignableFrom(obj.getClass())) {
                    LOGGER.error(
                            "A \"{}\" object is not assignable to a \"{}\" variable", className, superClass.getName());
                    return defaultValue;
                }
                return obj;
            } catch (final ReflectiveOperationException e) {
                LOGGER.error("Could not instantiate class [" + className + "].", e);
            }
        }
        return defaultValue;
    }

    public static Object instantiateByKey(
            final Properties props, final String key, final Class superClass, final Object defaultValue) {

        // Get the value of the property in string form
        final String className = findAndSubst(key, props);
        if (className == null) {
            LogLog.error("Could not find value for key " + key);
            return defaultValue;
        }
        // Trim className to avoid trailing spaces that cause problems.
        return OptionConverter.instantiateByClassName(className.trim(), superClass, defaultValue);
    }

    /**
     * Configure log4j given an {@link InputStream}.
     * <p>
     * The InputStream will be interpreted by a new instance of a log4j configurator.
     * </p>
     * <p>
     * All configurations steps are taken on the <code>hierarchy</code> passed as a parameter.
     * </p>
     *
     * @param inputStream The configuration input stream.
     * @param clazz The class name, of the log4j configurator which will parse the <code>inputStream</code>. This must be a
     *        subclass of {@link Configurator}, or null. If this value is null then a default configurator of
     *        {@link PropertyConfigurator} is used.
     * @param hierarchy The {@link LoggerRepository} to act on.
     * @since 1.2.17
     */
    public static void selectAndConfigure(
            final InputStream inputStream, final String clazz, final LoggerRepository hierarchy) {
        Configurator configurator = null;

        if (clazz != null) {
            LOGGER.debug("Preferred configurator class: " + clazz);
            configurator = (Configurator) instantiateByClassName(clazz, Configurator.class, null);
            if (configurator == null) {
                LOGGER.error("Could not instantiate configurator [" + clazz + "].");
                return;
            }
        } else {
            configurator = new PropertyConfigurator();
        }

        configurator.doConfigure(inputStream, hierarchy);
    }

    /**
     * Configure log4j given a URL.
     * <p>
     * The url must point to a file or resource which will be interpreted by a new instance of a log4j configurator.
     * </p>
     * <p>
     * All configurations steps are taken on the <code>hierarchy</code> passed as a parameter.
     * </p>
     *
     * @param url The location of the configuration file or resource.
     * @param clazz The classname, of the log4j configurator which will parse the file or resource at <code>url</code>. This
     *        must be a subclass of {@link Configurator}, or null. If this value is null then a default configurator of
     *        {@link PropertyConfigurator} is used, unless the filename pointed to by <code>url</code> ends in '.xml', in
     *        which case {@link org.apache.log4j.xml.DOMConfigurator} is used.
     * @param hierarchy The {@link LoggerRepository} to act on.
     *
     * @since 1.1.4
     */
    public static void selectAndConfigure(final URL url, String clazz, final LoggerRepository hierarchy) {
        Configurator configurator = null;
        final String filename = url.getFile();

        if (clazz == null && filename != null && filename.endsWith(".xml")) {
            clazz = "org.apache.log4j.xml.DOMConfigurator";
        }

        if (clazz != null) {
            LOGGER.debug("Preferred configurator class: " + clazz);
            configurator = (Configurator) instantiateByClassName(clazz, Configurator.class, null);
            if (configurator == null) {
                LOGGER.error("Could not instantiate configurator [" + clazz + "].");
                return;
            }
        } else {
            configurator = new PropertyConfigurator();
        }

        configurator.doConfigure(url, hierarchy);
    }

    /**
     * Perform variable substitution in string <code>val</code> from the
     * values of keys found in the system propeties.
     *
     * <p>The variable substitution delimeters are <b>${</b> and <b>}</b>.
     *
     * <p>For example, if the System properties contains "key=value", then
     * the call
     * <pre>
     * String s = OptionConverter.substituteVars("Value of key is ${key}.");
     * </pre>
     * <p>
     * will set the variable <code>s</code> to "Value of key is value.".
     *
     * <p>If no value could be found for the specified key, then the
     * <code>props</code> parameter is searched, if the value could not
     * be found there, then substitution defaults to the empty string.
     *
     * <p>For example, if system propeties contains no value for the key
     * "inexistentKey", then the call
     *
     * <pre>
     * String s = OptionConverter.subsVars("Value of inexistentKey is [${inexistentKey}]");
     * </pre>
     * will set <code>s</code> to "Value of inexistentKey is []"
     *
     * <p>An {@link IllegalArgumentException} is thrown if
     * <code>val</code> contains a start delimeter "${" which is not
     * balanced by a stop delimeter "}". </p>
     *
     * <p><b>Author</b> Avy Sharell</p>
     *
     * @param val The string on which variable substitution is performed.
     * @param props The properties to use for the substitution.
     * @return The substituted string.
     * @throws IllegalArgumentException if <code>val</code> is malformed.
     */
    public static String substVars(final String val, final Properties props) throws IllegalArgumentException {
        return substVars(val, props, new ArrayList<>());
    }

    private static String substVars(final String val, final Properties props, final List<String> keys)
            throws IllegalArgumentException {
        if (val == null) {
            return null;
        }
        final StringBuilder sbuf = new StringBuilder();

        int i = 0;
        int j;
        int k;

        while (true) {
            j = val.indexOf(DELIM_START, i);
            if (j == -1) {
                // no more variables
                if (i == 0) { // this is a simple string
                    return val;
                }
                // add the tail string which contails no variables and return the result.
                sbuf.append(val.substring(i));
                return sbuf.toString();
            }
            sbuf.append(val.substring(i, j));
            k = val.indexOf(DELIM_STOP, j);
            if (k == -1) {
                throw new IllegalArgumentException(
                        Strings.dquote(val) + " has no closing brace. Opening brace at position " + j + '.');
            }
            j += DELIM_START_LEN;
            final String key = val.substring(j, k);
            // first try in System properties
            String replacement = PropertiesUtil.getProperties().getStringProperty(key, null);
            // then try props parameter
            if (replacement == null && props != null) {
                replacement = props.getProperty(key);
            }

            if (replacement != null) {

                // Do variable substitution on the replacement string
                // such that we can solve "Hello ${x2}" as "Hello p1"
                // the where the properties are
                // x1=p1
                // x2=${x1}
                if (!keys.contains(key)) {
                    final List<String> usedKeys = new ArrayList<>(keys);
                    usedKeys.add(key);
                    final String recursiveReplacement = substVars(replacement, props, usedKeys);
                    sbuf.append(recursiveReplacement);
                } else {
                    sbuf.append(replacement);
                }
            }
            i = k + DELIM_STOP_LEN;
        }
    }

    /**
     * If <code>value</code> is "true", then <code>true</code> is
     * returned. If <code>value</code> is "false", then
     * <code>true</code> is returned. Otherwise, <code>default</code> is
     * returned.
     *
     * <p>Case of value is unimportant.
     * @param value The value to convert.
     * @param dEfault The default value.
     * @return the value of the result.
     */
    public static boolean toBoolean(final String value, final boolean dEfault) {
        if (value == null) {
            return dEfault;
        }
        final String trimmedVal = value.trim();
        if ("true".equalsIgnoreCase(trimmedVal)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmedVal)) {
            return false;
        }
        return dEfault;
    }

    public static long toFileSize(final String value, final long defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        String s = toRootUpperCase(value.trim());
        long multiplier = 1;
        int index;

        if ((index = s.indexOf("KB")) != -1) {
            multiplier = 1024;
            s = s.substring(0, index);
        } else if ((index = s.indexOf("MB")) != -1) {
            multiplier = 1024 * 1024;
            s = s.substring(0, index);
        } else if ((index = s.indexOf("GB")) != -1) {
            multiplier = 1024 * 1024 * 1024;
            s = s.substring(0, index);
        }
        if (s != null) {
            try {
                return Long.valueOf(s).longValue() * multiplier;
            } catch (final NumberFormatException e) {
                LogLog.error("[" + s + "] is not in proper int form.");
                LogLog.error("[" + value + "] not in expected format.", e);
            }
        }
        return defaultValue;
    }

    public static int toInt(final String value, final int dEfault) {
        if (value != null) {
            final String s = value.trim();
            try {
                return Integer.valueOf(s).intValue();
            } catch (final NumberFormatException e) {
                LogLog.error("[" + s + "] is not in proper int form.");
                e.printStackTrace();
            }
        }
        return dEfault;
    }

    /**
     * Converts a standard or custom priority level to a Level object.
     * <p>
     * If <code>value</code> is of form "level#classname", then the specified class'
     * toLevel method is called to process the specified level string; if no '#'
     * character is present, then the default {@link org.apache.log4j.Level} class
     * is used to process the level value.
     * </p>
     *
     * <p>
     * As a special case, if the <code>value</code> parameter is equal to the string
     * "NULL", then the value <code>null</code> will be returned.
     * </p>
     *
     * <p>
     * As a Log4j 2.x extension, a {@code value}
     * "level#org.apache.logging.log4j.Level" retrieves the corresponding custom
     * Log4j 2.x level.
     * </p>
     *
     * <p>
     * If any error occurs while converting the value to a level, the
     * <code>defaultValue</code> parameter, which may be <code>null</code>, is
     * returned.
     * </p>
     *
     * <p>
     * Case of <code>value</code> is insignificant for the level, but is
     * significant for the class name part, if present.
     * </p>
     *
     * @param value        The value to convert.
     * @param defaultValue The default value.
     * @return the value of the result.
     *
     * @since 1.1
     */
    public static Level toLevel(String value, final Level defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        value = value.trim();
        final Level cached = LEVELS.get(value);
        if (cached != null) {
            return cached;
        }

        final int hashIndex = value.indexOf('#');
        if (hashIndex == -1) {
            if ("NULL".equalsIgnoreCase(value)) {
                return null;
            }
            // no class name specified : use standard Level class
            final Level standardLevel = Level.toLevel(value, defaultValue);
            if (standardLevel != null && value.equals(standardLevel.toString())) {
                LEVELS.putIfAbsent(value, standardLevel);
            }
            return standardLevel;
        }

        final String clazz = value.substring(hashIndex + 1);
        final String levelName = value.substring(0, hashIndex);

        final Level customLevel = toLevel(clazz, levelName, defaultValue);
        if (customLevel != null
                && levelName.equals(customLevel.toString())
                && clazz.equals(customLevel.getClass().getName())) {
            LEVELS.putIfAbsent(value, customLevel);
        }
        return customLevel;
    }

    /**
     * Converts a custom priority level to a Level object.
     *
     * <p>
     * If {@code clazz} has the special value "org.apache.logging.log4j.Level" a
     * wrapper of the corresponding Log4j 2.x custom level object is returned.
     * </p>
     *
     * @param clazz        a custom level class,
     * @param levelName    the name of the level,
     * @param defaultValue the value to return in case an error occurs,
     * @return the value of the result.
     */
    public static Level toLevel(final String clazz, final String levelName, final Level defaultValue) {

        // This is degenerate case but you never know.
        if ("NULL".equalsIgnoreCase(levelName)) {
            return null;
        }

        LOGGER.debug("toLevel" + ":class=[" + clazz + "]" + ":pri=[" + levelName + "]");

        // Support for levels defined in Log4j2.
        if (LOG4J2_LEVEL_CLASS.equals(clazz)) {
            final org.apache.logging.log4j.Level v2Level =
                    org.apache.logging.log4j.Level.getLevel(toRootUpperCase(levelName));
            if (v2Level != null) {
                return new LevelWrapper(v2Level);
            }
            return defaultValue;
        }
        try {
            final Class<?> customLevel = LoaderUtil.loadClass(clazz);

            // get a ref to the specified class' static method
            // toLevel(String, org.apache.log4j.Level)
            final Class<?>[] paramTypes = new Class[] {String.class, org.apache.log4j.Level.class};
            final java.lang.reflect.Method toLevelMethod = customLevel.getMethod("toLevel", paramTypes);

            // now call the toLevel method, passing level string + default
            final Object[] params = new Object[] {levelName, defaultValue};
            final Object o = toLevelMethod.invoke(null, params);

            return (Level) o;
        } catch (final ClassNotFoundException e) {
            LOGGER.warn("custom level class [" + clazz + "] not found.");
        } catch (final NoSuchMethodException e) {
            LOGGER.warn(
                    "custom level class [" + clazz + "]" + " does not have a class function toLevel(String, Level)", e);
        } catch (final java.lang.reflect.InvocationTargetException e) {
            if (e.getTargetException() instanceof InterruptedException
                    || e.getTargetException() instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.warn("custom level class [" + clazz + "]" + " could not be instantiated", e);
        } catch (final ClassCastException e) {
            LOGGER.warn("class [" + clazz + "] is not a subclass of org.apache.log4j.Level", e);
        } catch (final IllegalAccessException e) {
            LOGGER.warn("class [" + clazz + "] cannot be instantiated due to access restrictions", e);
        } catch (final RuntimeException e) {
            LOGGER.warn("class [" + clazz + "], level [" + levelName + "] conversion failed.", e);
        }
        return defaultValue;
    }

    /**
     * OptionConverter is a static class.
     */
    private OptionConverter() {}

    private static class LevelWrapper extends Level {

        private static final long serialVersionUID = -7693936267612508528L;

        protected LevelWrapper(final org.apache.logging.log4j.Level v2Level) {
            super(toLog4j1Level(v2Level.intLevel()), v2Level.name(), toSyslogLevel(v2Level.intLevel()), v2Level);
        }
    }
}
