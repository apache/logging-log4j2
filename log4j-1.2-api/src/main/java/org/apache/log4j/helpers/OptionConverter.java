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

import org.apache.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.LoaderUtil;

import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * A convenience class to convert property values to specific types.
 */
public class OptionConverter {

    static String DELIM_START = "${";
    static char DELIM_STOP = '}';
    static int DELIM_START_LEN = 2;
    static int DELIM_STOP_LEN = 1;
    private static final Logger LOGGER = LogManager.getLogger(OptionConverter.class);
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

    /**
     * OptionConverter is a static class.
     */
    private OptionConverter() {
    }

    public static String[] concatanateArrays(String[] l, String[] r) {
        int len = l.length + r.length;
        String[] a = new String[len];

        System.arraycopy(l, 0, a, 0, l.length);
        System.arraycopy(r, 0, a, l.length, r.length);

        return a;
    }

    public static String convertSpecialChars(String s) {
        char c;
        int len = s.length();
        StringBuilder sbuf = new StringBuilder(len);

        int i = 0;
        while (i < len) {
            c = s.charAt(i++);
            if (c == '\\') {
                c = s.charAt(i++);
                for (CharMap entry : charMap) {
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
     * Very similar to <code>System.getProperty</code> except
     * that the {@link SecurityException} is hidden.
     *
     * @param key The key to search for.
     * @param def The default value to return.
     * @return the string value of the system property, or the default
     * value if there is no property with that key.
     * @since 1.1
     */
    public static String getSystemProperty(String key, String def) {
        try {
            return System.getProperty(key, def);
        } catch (Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
            LOGGER.debug("Was not allowed to read system property \"{}\".", key);
            return def;
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
    public static boolean toBoolean(String value, boolean dEfault) {
        if (value == null) {
            return dEfault;
        }
        String trimmedVal = value.trim();
        if ("true".equalsIgnoreCase(trimmedVal)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmedVal)) {
            return false;
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
    public static Level toLevel(String value, Level defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        value = value.trim();

        int hashIndex = value.indexOf('#');
        if (hashIndex == -1) {
            if ("NULL".equalsIgnoreCase(value)) {
                return null;
            }
            // no class name specified : use standard Level class
            return Level.toLevel(value, defaultValue);
        }

        Level result = defaultValue;

        String clazz = value.substring(hashIndex + 1);
        String levelName = value.substring(0, hashIndex);

        // This is degenerate case but you never know.
        if ("NULL".equalsIgnoreCase(levelName)) {
            return null;
        }

        LOGGER.debug("toLevel" + ":class=[" + clazz + "]"
                + ":pri=[" + levelName + "]");

        try {
            Class<?> customLevel = LoaderUtil.loadClass(clazz);

            // get a ref to the specified class' static method
            // toLevel(String, org.apache.log4j.Level)
            Class<?>[] paramTypes = new Class[] { String.class, org.apache.log4j.Level.class };
            java.lang.reflect.Method toLevelMethod =
                    customLevel.getMethod("toLevel", paramTypes);

            // now call the toLevel method, passing level string + default
            Object[] params = new Object[]{levelName, defaultValue};
            Object o = toLevelMethod.invoke(null, params);

            result = (Level) o;
        } catch (ClassNotFoundException e) {
            LOGGER.warn("custom level class [" + clazz + "] not found.");
        } catch (NoSuchMethodException e) {
            LOGGER.warn("custom level class [" + clazz + "]"
                    + " does not have a class function toLevel(String, Level)", e);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getTargetException() instanceof InterruptedException
                    || e.getTargetException() instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.warn("custom level class [" + clazz + "]"
                    + " could not be instantiated", e);
        } catch (ClassCastException e) {
            LOGGER.warn("class [" + clazz
                    + "] is not a subclass of org.apache.log4j.Level", e);
        } catch (IllegalAccessException e) {
            LOGGER.warn("class [" + clazz +
                    "] cannot be instantiated due to access restrictions", e);
        } catch (RuntimeException e) {
            LOGGER.warn("class [" + clazz + "], level [" + levelName +
                    "] conversion failed.", e);
        }
        return result;
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
    public static Object instantiateByClassName(String className, Class<?> superClass,
            Object defaultValue) {
        if (className != null) {
            try {
                Object obj = LoaderUtil.newInstanceOf(className);
                if (!superClass.isAssignableFrom(obj.getClass())) {
                    LOGGER.error("A \"{}\" object is not assignable to a \"{}\" variable", className,
                            superClass.getName());
                    return defaultValue;
                }
                return obj;
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                    | InstantiationException | InvocationTargetException e) {
                LOGGER.error("Could not instantiate class [" + className + "].", e);
            }
        }
        return defaultValue;
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
    public static String substVars(String val, Properties props) throws IllegalArgumentException {

        StringBuilder sbuf = new StringBuilder();

        int i = 0;
        int j, k;

        while (true) {
            j = val.indexOf(DELIM_START, i);
            if (j == -1) {
                // no more variables
                if (i == 0) { // this is a simple string
                    return val;
                }
                sbuf.append(val.substring(i, val.length()));
                return sbuf.toString();
            }
            sbuf.append(val.substring(i, j));
            k = val.indexOf(DELIM_STOP, j);
            if (k == -1) {
                throw new IllegalArgumentException('"' + val +
                        "\" has no closing brace. Opening brace at position " + j
                        + '.');
            }
            j += DELIM_START_LEN;
            String key = val.substring(j, k);
            // first try in System properties
            String replacement = getSystemProperty(key, null);
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
                String recursiveReplacement = substVars(replacement, props);
                sbuf.append(recursiveReplacement);
            }
            i = k + DELIM_STOP_LEN;
        }
    }

    public static org.apache.logging.log4j.Level convertLevel(String level,
            org.apache.logging.log4j.Level defaultLevel) {
        Level l = toLevel(level, null);
        return l != null ? convertLevel(l) : defaultLevel;
    }

    public static  org.apache.logging.log4j.Level convertLevel(Level level) {
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

    public static Level convertLevel(org.apache.logging.log4j.Level level) {
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

    /**
     * Find the value corresponding to <code>key</code> in
     * <code>props</code>. Then perform variable substitution on the
     * found value.
     * @param key The key used to locate the substitution string.
     * @param props The properties to use in the substitution.
     * @return The substituted string.
     */
    public static String findAndSubst(String key, Properties props) {
        String value = props.getProperty(key);
        if (value == null) {
            return null;
        }

        try {
            return substVars(value, props);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Bad option value [{}].", value, e);
            return value;
        }
    }

    private static class CharMap {
        final char key;
        final char replacement;

        public CharMap(char key, char replacement) {
            this.key = key;
            this.replacement = replacement;
        }
    }
}
