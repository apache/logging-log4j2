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
package org.apache.logging.log4j.core.util;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * A convenience class to convert property values to specific types.
 */
public final class OptionConverter {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final String DELIM_START = "${";
    private static final char DELIM_STOP = '}';
    private static final int DELIM_START_LEN = 2;
    private static final int DELIM_STOP_LEN = 1;
    private static final int ONE_K = 1024;

    /**
     * OptionConverter is a static class.
     */
    private OptionConverter() {}

    public static String[] concatenateArrays(final String[] l, final String[] r) {
        final int len = l.length + r.length;
        final String[] a = new String[len];

        System.arraycopy(l, 0, a, 0, l.length);
        System.arraycopy(r, 0, a, l.length, r.length);

        return a;
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
                switch (c) {
                    case 'n':
                        c = '\n';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'f':
                        c = '\f';
                        break;
                    case 'b':
                        c = '\b';
                        break;
                    case '"':
                        c = '\"';
                        break;
                    case '\'':
                        c = '\'';
                        break;
                    case '\\':
                        c = '\\';
                        break;
                    default:
                        // there is no default case.
                }
            }
            sbuf.append(c);
        }
        return sbuf.toString();
    }

    public static Object instantiateByKey(
            final Properties props, final String key, final Class<?> superClass, final Object defaultValue) {

        // Get the value of the property in string form
        final String className = findAndSubst(key, props);
        if (className == null) {
            LOGGER.error("Could not find value for key {}", key);
            return defaultValue;
        }
        // Trim className to avoid trailing spaces that cause problems.
        return OptionConverter.instantiateByClassName(className.trim(), superClass, defaultValue);
    }

    /**
     * If <code>value</code> is "true", then {@code true} is
     * returned. If <code>value</code> is "false", then
     * {@code false} is returned. Otherwise, <code>default</code> is
     * returned.
     *
     * <p>Case of value is unimportant.</p>
     * @param value The value to convert.
     * @param defaultValue The default value.
     * @return true or false, depending on the value and/or default.
     */
    public static boolean toBoolean(final String value, final boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        final String trimmedVal = value.trim();
        if ("true".equalsIgnoreCase(trimmedVal)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmedVal)) {
            return false;
        }
        return defaultValue;
    }

    /**
     * Convert the String value to an int.
     * @param value The value as a String.
     * @param defaultValue The default value.
     * @return The value as an int.
     */
    public static int toInt(final String value, final int defaultValue) {
        if (value != null) {
            final String s = value;
            try {
                return Integers.parseInt(s);
            } catch (final NumberFormatException e) {
                LOGGER.error("[{}] is not in proper int form.", s, e);
            }
        }
        return defaultValue;
    }

    public static Level toLevel(String value, Level defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        value = value.trim();

        final int hashIndex = value.indexOf('#');
        if (hashIndex == -1) {
            if ("NULL".equalsIgnoreCase(value)) {
                return null;
            } else {
                // no class name specified : use standard Level class
                return Level.toLevel(value, defaultValue);
            }
        }

        Level result = defaultValue;

        final String clazz = value.substring(hashIndex + 1);
        final String levelName = value.substring(0, hashIndex);

        // This is degenerate case but you never know.
        if ("NULL".equalsIgnoreCase(levelName)) {
            return null;
        }

        LOGGER.debug("toLevel" + ":class=[" + clazz + "]" + ":pri=[" + levelName + "]");

        try {
            final Class<?> customLevel = Loader.loadClass(clazz);

            // get a ref to the specified class' static method
            // toLevel(String, org.apache.log4j.Level)
            final Class<?>[] paramTypes = new Class[] {String.class, Level.class};
            final java.lang.reflect.Method toLevelMethod = customLevel.getMethod("toLevel", paramTypes);

            // now call the toLevel method, passing level string + default
            final Object[] params = new Object[] {levelName, defaultValue};
            final Object o = toLevelMethod.invoke(null, params);

            result = (Level) o;
        } catch (ClassNotFoundException e) {
            LOGGER.warn("custom level class [" + clazz + "] not found.");
        } catch (NoSuchMethodException e) {
            LOGGER.warn(
                    "custom level class [" + clazz + "]" + " does not have a class function toLevel(String, Level)", e);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getTargetException() instanceof InterruptedException
                    || e.getTargetException() instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.warn("custom level class [" + clazz + "]" + " could not be instantiated", e);
        } catch (ClassCastException e) {
            LOGGER.warn("class [" + clazz + "] is not a subclass of org.apache.log4j.Level", e);
        } catch (IllegalAccessException e) {
            LOGGER.warn("class [" + clazz + "] cannot be instantiated due to access restrictions", e);
        } catch (RuntimeException e) {
            LOGGER.warn("class [" + clazz + "], level [" + levelName + "] conversion failed.", e);
        }
        return result;
    }

    /**
     *
     * @param value The size of the file as a String.
     * @param defaultValue The default value.
     * @return The size of the file as a long.
     */
    public static long toFileSize(final String value, final long defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        String str = toRootUpperCase(value.trim());
        long multiplier = 1;
        int index;

        if ((index = str.indexOf("KB")) != -1) {
            multiplier = ONE_K;
            str = str.substring(0, index);
        } else if ((index = str.indexOf("MB")) != -1) {
            multiplier = ONE_K * ONE_K;
            str = str.substring(0, index);
        } else if ((index = str.indexOf("GB")) != -1) {
            multiplier = ONE_K * ONE_K * ONE_K;
            str = str.substring(0, index);
        }
        try {
            return Long.parseLong(str) * multiplier;
        } catch (final NumberFormatException e) {
            LOGGER.error("[{}] is not in proper int form.", str);
            LOGGER.error("[{}] not in expected format.", value, e);
        }
        return defaultValue;
    }

    /**
     * Find the value corresponding to <code>key</code> in
     * <code>props</code>. Then perform variable substitution on the
     * found value.
     * @param key The key to locate.
     * @param props The properties.
     * @return The String after substitution.
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
                final Class<?> classObj = Loader.loadClass(className);
                if (!superClass.isAssignableFrom(classObj)) {
                    LOGGER.error(
                            "A \"{}\" object is not assignable to a \"{}\" variable.", className, superClass.getName());
                    LOGGER.error(
                            "The class \"{}\" was loaded by [{}] whereas object of type [{}] was loaded by [{}].",
                            superClass.getName(),
                            superClass.getClassLoader(),
                            classObj.getTypeName(),
                            classObj.getName());
                    return defaultValue;
                }
                return LoaderUtil.newInstanceOf(classObj);
            } catch (final Exception e) {
                LOGGER.error("Could not instantiate class [{}].", className, e);
            }
        }
        return defaultValue;
    }

    /**
     * Perform variable substitution in string <code>val</code> from the
     * values of keys found in the system propeties.
     *
     * <p>The variable substitution delimiters are <b>${</b> and <b>}</b>.</p>
     *
     * <p>For example, if the System properties contains "key=value", then
     * the call</p>
     * <pre>
     * String s = OptionConverter.substituteVars("Value of key is ${key}.");
     * </pre>
     * <p>
     * will set the variable <code>s</code> to "Value of key is value.".
     * </p>
     * <p>If no value could be found for the specified key, then the
     * <code>props</code> parameter is searched, if the value could not
     * be found there, then substitution defaults to the empty string.</p>
     *
     * <p>For example, if system properties contains no value for the key
     * "inexistentKey", then the call
     * </p>
     * <pre>
     * String s = OptionConverter.subsVars("Value of inexistentKey is [${inexistentKey}]");
     * </pre>
     * <p>
     * will set <code>s</code> to "Value of inexistentKey is []"
     * </p>
     * <p>An {@link java.lang.IllegalArgumentException} is thrown if
     * <code>val</code> contains a start delimeter "${" which is not
     * balanced by a stop delimeter "}". </p>
     *
     * @param val The string on which variable substitution is performed.
     * @param props The properties to use for substitution.
     * @return The String after substitution.
     * @throws IllegalArgumentException if <code>val</code> is malformed.
     */
    public static String substVars(final String val, final Properties props) throws IllegalArgumentException {
        return substVars(val, props, new ArrayList<>());
    }

    private static String substVars(final String val, final Properties props, final List<String> keys)
            throws IllegalArgumentException {

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
                sbuf.append(val.substring(i, val.length()));
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
}
