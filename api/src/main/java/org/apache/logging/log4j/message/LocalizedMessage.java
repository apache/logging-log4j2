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
package org.apache.logging.log4j.message;

import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This class is not the recommended way to Localize messages. It is provided to provide some level
 * of compatibility with Log4j 1.x.
 *
 * The recommended way to localize messages is to simply log a message id. Log events should
 * then be recorded without formatting into some kind of data store. The application that is
 * used to read the events and display them to the user should also localize and format the
 * messages for the end user.
 */
public class LocalizedMessage extends ParameterizedMessage implements LoggerNameAwareMessage
{
    private static final long serialVersionUID = 3893703791567290742L;

    private String bundleId;

    private transient ResourceBundle bundle;

    private Locale locale;

    private transient StatusLogger logger = StatusLogger.getLogger();

    private String loggerName;

    public LocalizedMessage(final String messagePattern, final String[] stringArgs, final Throwable throwable) {
        super(messagePattern, stringArgs, throwable);
        setup(null, null, null);
    }


    public LocalizedMessage(final String bundleId, final String key, final String[] stringArgs,
                            final Throwable throwable) {
        super(key, stringArgs, throwable);
        setup(bundleId, null, null);
    }

    public LocalizedMessage(final ResourceBundle bundle, final String key, final String[] stringArgs,
                            final Throwable throwable) {
        super(key, stringArgs, throwable);
        setup(null, bundle, null);
    }

    public LocalizedMessage(final String bundleId, final Locale locale, final String key, final String[] stringArgs,
                            final Throwable throwable) {
        super(key, stringArgs, throwable);
        setup(bundleId, null, locale);
    }

    public LocalizedMessage(final ResourceBundle bundle, final Locale locale, final String key, final String[] stringArgs,
                            final Throwable throwable) {
        super(key, stringArgs, throwable);
        setup(null, bundle, locale);
    }

    public LocalizedMessage(final Locale locale, final String key, final String[] stringArgs, final Throwable throwable) {
        super(key, stringArgs, throwable);
        setup(null, null, locale);
    }


    /**
     * <p>This method returns a LocalizedMessage which contains the arguments converted to String
     * as well as an optional Throwable.</p>
     * <p/>
     * <p>If the last argument is a Throwable and is NOT used up by a placeholder in the message
     * pattern it is returned in LocalizedMessage.getThrowable() and won't be contained in the
     * created String[].<br/>
     * If it is used up ParameterizedMessage.getThrowable() will return null even if the last
     * argument was a Throwable!</p>
     *
     * @param messagePattern the message pattern that to be checked for placeholders.
     * @param arguments      the argument array to be converted.
     */
    public LocalizedMessage(final String messagePattern, final Object[] arguments) {
        super(messagePattern, arguments);
        setup(null, null, null);
    }

    public LocalizedMessage(final String bundleId, final String key, final Object[] arguments) {
        super(key, arguments);
        setup(bundleId, null, null);
    }

    public LocalizedMessage(final ResourceBundle bundle, final String key, final Object[] arguments) {
        super(key, arguments);
        setup(null, bundle, null);
    }

    public LocalizedMessage(final String bundleId, final Locale locale, final String key, final Object[] arguments) {
        super(key, arguments);
        setup(bundleId, null, locale);
    }

    public LocalizedMessage(final ResourceBundle bundle, final Locale locale, final String key, final Object[] arguments) {
        super(key, arguments);
        setup(null, bundle, locale);
    }

    public LocalizedMessage(final Locale locale, final String key, final Object[] arguments) {
        super(key, arguments);
        setup(null, null, locale);
    }

    public LocalizedMessage(final String messagePattern, final Object arg) {
        super(messagePattern, arg);
        setup(null, null, null);
    }

    public LocalizedMessage(final String bundleId, final String key, final Object arg) {
        super(key, arg);
        setup(bundleId, null, null);
    }

    public LocalizedMessage(final ResourceBundle bundle, final String key, final Object arg) {
        super(key, arg);
        setup(null, bundle, null);
    }

    public LocalizedMessage(final String bundleId, final Locale locale, final String key, final Object arg) {
        super(key, arg);
        setup(bundleId, null, locale);
    }

    public LocalizedMessage(final ResourceBundle bundle, final Locale locale, final String key, final Object arg) {
        super(key, arg);
        setup(null, bundle, locale);
    }

    public LocalizedMessage(final Locale locale, final String key, final Object arg) {
        super(key, arg);
        setup(null, null, locale);
    }

    public LocalizedMessage(final String messagePattern, final Object arg1, final Object arg2) {
        super(messagePattern, arg1, arg2);
        setup(null, null, null);
    }

    public LocalizedMessage(final String bundleId, final String key, final Object arg1, final Object arg2) {
        super(key, arg1, arg2);
        setup(bundleId, null, null);
    }

    public LocalizedMessage(final ResourceBundle bundle, final String key, final Object arg1, final Object arg2) {
        super(key, arg1, arg2);
        setup(null, bundle, null);
    }

    public LocalizedMessage(final String bundleId, final Locale locale, final String key, final Object arg1, final Object arg2) {
        super(key, arg1, arg2);
        setup(bundleId, null, locale);
    }

    public LocalizedMessage(final ResourceBundle bundle, final Locale locale, final String key, final Object arg1,
                            final Object arg2) {
        super(key, arg1, arg2);
        setup(null, bundle, locale);
    }

    public LocalizedMessage(final Locale locale, final String key, final Object arg1, final Object arg2) {
        super(key, arg1, arg2);
        setup(null, null, locale);
    }

    /**
     * Set the name of the Logger.
     * @param name The name of the Logger.
     */
    public void setLoggerName(final String name) {
        this.loggerName = name;
    }

    /**
     * Returns the name of the Logger.
     * @return the name of the Logger.
     */
    public String getLoggerName() {
        return this.loggerName;
    }

    private void setup(final String bundleId, final ResourceBundle bundle, final Locale locale) {
        this.bundleId = bundleId;
        this.bundle = bundle;
        this.locale = locale;
    }

    /**
     * Returns the formatted message after looking up the format in the resource bundle.
     * @param messagePattern The key for the resource bundle or the pattern if the bundle doesn't contain the key.
     * @param args The parameters.
     * @return The formatted message String.
     */
    @Override
    public String formatMessage(final String messagePattern, final String[] args) {
        ResourceBundle bundle = this.bundle;
        if (bundle == null) {
            if (bundleId != null) {
                bundle = getBundle(bundleId, locale, false);
            } else {
                bundle = getBundle(loggerName, locale, true);
            }
        }
        final String msgPattern = (bundle == null || !bundle.containsKey(messagePattern)) ?
            messagePattern : bundle.getString(messagePattern);
        return format(msgPattern, args);
    }

    /**
     * Override this to use a ResourceBundle.Control in Java 6
     * @param key The key to the bundle.
     * @param locale The locale to use when formatting the message.
     * @param loop If true the key will be treated as a package or class name and a resource bundle will
     * be located based on all or part of the package name. If false the key is expected to be the exact bundle id.
     * @return The ResourceBundle.
     */
    protected ResourceBundle getBundle(final String key, final Locale locale, final boolean loop) {
        ResourceBundle rb = null;

        if (key == null) {
            return null;
        }
        try {
            if (locale != null) {
                rb = ResourceBundle.getBundle(key, locale);
            } else {
                rb = ResourceBundle.getBundle(key);
            }
        } catch (final MissingResourceException ex) {
            if (!loop) {
                logger.debug("Unable to locate ResourceBundle " + key);
                return null;
            }
        }

        String substr = key;
        int i;
        while (rb == null && (i = substr.lastIndexOf('.')) > 0) {
            substr = substr.substring(0, i);
            try {
                if (locale != null) {
                    rb = ResourceBundle.getBundle(substr, locale);
                } else {
                    rb = ResourceBundle.getBundle(substr);
                }
            } catch (final MissingResourceException ex) {
                logger.debug("Unable to locate ResourceBundle " + substr);
            }
        }
        return rb;
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        bundle = null;
        logger = StatusLogger.getLogger();
    }
}
