/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.Configurator;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.logging.log4j.Logger;
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

    public static  org.apache.logging.log4j.Level convertLevel(final Level level) {
        if (level == null) {
            return org.apache.logging.log4j.Level.ERROR;
        }
        if (level.isGreaterOrEqual(Level.FATAL)) {
            return org.apache.logging.log4j.Level.FATAL;
        } else if (level.isGreaterOrEqual(Level.ERROR)) {
            return org.apache.logging.log4j.Level.ERROR;
        } else if (level.isGreaterOrEqual(Level.WARN)) {
            return org.apache.logging.log4j.Level.WARN;
        } else if (level.isGreaterOrEqual(Level.INFO)) {
            return org.apache.logging.log4j.Level.INFO;
        } else if (level.isGreaterOrEqual(Level.DEBUG)) {
            return org.apache.logging.log4j.Level.DEBUG;
        } else if (level.isGreaterOrEqual(Level.TRACE)) {
            return org.apache.logging.log4j.Level.TRACE;
        }
        return org.apache.logging.log4j.Level.ALL;
    }


    public static Level convertLevel(final org.apache.logging.log4j.Level level) {
        if (level == null) {
            return Level.ERROR;
        }
        switch (level.getStandardLevel()) {
            case FATAL:
                return Level.FATAL;
            case WARN:
                return Level.WARN;
            case INFO:
                return Level.INFO;
            case DEBUG:
                return Level.DEBUG;
            case TRACE:
                return Level.TRACE;
            case ALL:
                return Level.ALL;
            case OFF:
                return Level.OFF;
            default:
                return Level.ERROR;
        }
    }

    public static org.apache.logging.log4j.Level convertLevel(final String level,
            final org.apache.logging.log4j.Level defaultLevel) {
        final Level l = toLevel(level, null);
        return l != null ? convertLevel(l) : defaultLevel;
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
    public static Object instantiateByClassName(final String className, final Class<?> superClass,
            final Object defaultValue) {
        if (className != null) {
            try {
                final Object obj = LoaderUtil.newInstanceOf(className);
                if (!superClass.isAssignableFrom(obj.getClass())) {
                    LOGGER.error("A \"{}\" object is not assignable to a \"{}\" variable", className,
                            superClass.getName());
                    return defaultValue;
                }
                return obj;
            } catch (final ReflectiveOperationException e) {
                LOGGER.error("Could not instantiate class [" + className + "].", e);
            }
        }
        return defaultValue;
    }

    public static Object instantiateByKey(final Properties props, final String key, final Class superClass, final Object defaultValue) {

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
    static public void selectAndConfigure(final InputStream inputStream, final String clazz, final LoggerRepository hierarchy) {
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
    static public void selectAndConfigure(final URL url, String clazz, final LoggerRepository hierarchy) {
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
                throw new IllegalArgumentException(Strings.dquote(val)
                        + " has no closing brace. Opening brace at position " + j
                        + '.');
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

        String s = value.trim().toUpperCase();
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
     * Converts a standard or custom priority level to a Level
     * object.  <p> If <code>value</code> is of form
     * "level#classname", then the specified class' toLevel method
     * is called to process the specified level string; if no '#'
     * character is present, then the default {@link org.apache.log4j.Level}
     * class is used to process the level value.
     *
     * <p>As a special case, if the <code>value</code> parameter is
     * equal to the string "NULL", then the value <code>null</code> will
     * be returned.
     *
     * <p> If any error occurs while converting the value to a level,
     * the <code>defaultValue</code> parameter, which may be
     * <code>null</code>, is returned.
     *
     * <p> Case of <code>value</code> is insignificant for the level level, but is
     * significant for the class name part, if present.
     * @param value The value to convert.
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

        final int hashIndex = value.indexOf('#');
        if (hashIndex == -1) {
            if ("NULL".equalsIgnoreCase(value)) {
                return null;
            }
            // no class name specified : use standard Level class
            return Level.toLevel(value, defaultValue);
        }

        Level result = defaultValue;

        final String clazz = value.substring(hashIndex + 1);
        final String levelName = value.substring(0, hashIndex);

        // This is degenerate case but you never know.
        if ("NULL".equalsIgnoreCase(levelName)) {
            return null;
        }

        LOGGER.debug("toLevel" + ":class=[" + clazz + "]"
                + ":pri=[" + levelName + "]");

        try {
            final Class<?> customLevel = LoaderUtil.loadClass(clazz);

            // get a ref to the specified class' static method
            // toLevel(String, org.apache.log4j.Level)
            final Class<?>[] paramTypes = new Class[] { String.class, org.apache.log4j.Level.class };
            final java.lang.reflect.Method toLevelMethod =
                    customLevel.getMethod("toLevel", paramTypes);

            // now call the toLevel method, passing level string + default
            final Object[] params = new Object[]{levelName, defaultValue};
            final Object o = toLevelMethod.invoke(null, params);

            result = (Level) o;
        } catch (final ClassNotFoundException e) {
            LOGGER.warn("custom level class [" + clazz + "] not found.");
        } catch (final NoSuchMethodException e) {
            LOGGER.warn("custom level class [" + clazz + "]"
                    + " does not have a class function toLevel(String, Level)", e);
        } catch (final java.lang.reflect.InvocationTargetException e) {
            if (e.getTargetException() instanceof InterruptedException
                    || e.getTargetException() instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.warn("custom level class [" + clazz + "]"
                    + " could not be instantiated", e);
        } catch (final ClassCastException e) {
            LOGGER.warn("class [" + clazz
                    + "] is not a subclass of org.apache.log4j.Level", e);
        } catch (final IllegalAccessException e) {
            LOGGER.warn("class [" + clazz +
                    "] cannot be instantiated due to access restrictions", e);
        } catch (final RuntimeException e) {
            LOGGER.warn("class [" + clazz + "], level [" + levelName +
                    "] conversion failed.", e);
        }
        return result;
    }

    /**
     * OptionConverter is a static class.
     */
    private OptionConverter() {
    }
}
