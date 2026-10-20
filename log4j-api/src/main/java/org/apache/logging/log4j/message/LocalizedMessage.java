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
package org.apache.logging.log4j.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Provides some level of compatibility with Log4j 1.x and convenience but is not the recommended way to Localize
 * messages.
 * <p>
 * The recommended way to localize messages is to log a message id. Log events should then be recorded without
 * formatting into a data store. The application that is used to read the events and display them to the user can then
 * localize and format the messages for the end user.
 * </p>
 */
public class LocalizedMessage implements Message, LoggerNameAwareMessage {
    private static final long serialVersionUID = 3893703791567290742L;

    private String baseName;

    // ResourceBundle is not Serializable.
    private transient ResourceBundle resourceBundle;

    private final Locale locale;

    private transient StatusLogger logger = StatusLogger.getLogger();

    private String loggerName;
    private String key;
    private String[] stringArgs;
    private transient Object[] argArray;
    private String formattedMessage;
    private transient Throwable throwable;

    /**
     * Constructor with message pattern and arguments.
     *
     * @param messagePattern the message pattern that to be checked for placeholders.
     * @param arguments the argument array to be converted.
     */
    public LocalizedMessage(final String messagePattern, final Object[] arguments) {
        this((ResourceBundle) null, (Locale) null, messagePattern, arguments);
    }

    public LocalizedMessage(final String baseName, final String key, final Object[] arguments) {
        this(baseName, (Locale) null, key, arguments);
    }

    public LocalizedMessage(final ResourceBundle bundle, final String key, final Object[] arguments) {
        this(bundle, (Locale) null, key, arguments);
    }

    public LocalizedMessage(final String baseName, final Locale locale, final String key, final Object[] arguments) {
        this.key = key;
        this.argArray = arguments;
        this.throwable = null;
        this.baseName = baseName;
        this.resourceBundle = null;
        this.locale = locale;
    }

    public LocalizedMessage(
            final ResourceBundle bundle, final Locale locale, final String key, final Object[] arguments) {
        this.key = key;
        this.argArray = arguments;
        this.throwable = null;
        this.baseName = null;
        this.resourceBundle = bundle;
        this.locale = locale;
    }

    public LocalizedMessage(final Locale locale, final String key, final Object[] arguments) {
        this((ResourceBundle) null, locale, key, arguments);
    }

    public LocalizedMessage(final String messagePattern, final Object arg) {
        this((ResourceBundle) null, (Locale) null, messagePattern, new Object[] {arg});
    }

    public LocalizedMessage(final String baseName, final String key, final Object arg) {
        this(baseName, (Locale) null, key, new Object[] {arg});
    }

    /**
     * @since 2.8
     */
    public LocalizedMessage(final ResourceBundle bundle, final String key) {
        this(bundle, (Locale) null, key, new Object[] {});
    }

    public LocalizedMessage(final ResourceBundle bundle, final String key, final Object arg) {
        this(bundle, (Locale) null, key, new Object[] {arg});
    }

    public LocalizedMessage(final String baseName, final Locale locale, final String key, final Object arg) {
        this(baseName, locale, key, new Object[] {arg});
    }

    public LocalizedMessage(final ResourceBundle bundle, final Locale locale, final String key, final Object arg) {
        this(bundle, locale, key, new Object[] {arg});
    }

    public LocalizedMessage(final Locale locale, final String key, final Object arg) {
        this((ResourceBundle) null, locale, key, new Object[] {arg});
    }

    public LocalizedMessage(final String messagePattern, final Object arg1, final Object arg2) {
        this((ResourceBundle) null, (Locale) null, messagePattern, new Object[] {arg1, arg2});
    }

    public LocalizedMessage(final String baseName, final String key, final Object arg1, final Object arg2) {
        this(baseName, (Locale) null, key, new Object[] {arg1, arg2});
    }

    public LocalizedMessage(final ResourceBundle bundle, final String key, final Object arg1, final Object arg2) {
        this(bundle, (Locale) null, key, new Object[] {arg1, arg2});
    }

    public LocalizedMessage(
            final String baseName, final Locale locale, final String key, final Object arg1, final Object arg2) {
        this(baseName, locale, key, new Object[] {arg1, arg2});
    }

    public LocalizedMessage(
            final ResourceBundle bundle, final Locale locale, final String key, final Object arg1, final Object arg2) {
        this(bundle, locale, key, new Object[] {arg1, arg2});
    }

    public LocalizedMessage(final Locale locale, final String key, final Object arg1, final Object arg2) {
        this((ResourceBundle) null, locale, key, new Object[] {arg1, arg2});
    }

    /**
     * Set the name of the Logger.
     *
     * @param name The name of the Logger.
     */
    @Override
    public void setLoggerName(final String name) {
        this.loggerName = name;
    }

    /**
     * Returns the name of the Logger.
     *
     * @return the name of the Logger.
     */
    @Override
    public String getLoggerName() {
        return this.loggerName;
    }

    /**
     * Returns the formatted message after looking up the format in the resource bundle.
     *
     * @return The formatted message String.
     */
    @Override
    public String getFormattedMessage() {
        if (formattedMessage != null) {
            return formattedMessage;
        }
        ResourceBundle bundle = this.resourceBundle;
        if (bundle == null) {
            if (baseName != null) {
                bundle = getResourceBundle(baseName, locale, false);
            } else {
                bundle = getResourceBundle(loggerName, locale, true);
            }
        }
        final String myKey = getFormat();
        final String msgPattern = (bundle == null || !bundle.containsKey(myKey)) ? myKey : bundle.getString(myKey);
        final Object[] array = argArray == null ? stringArgs : argArray;
        final FormattedMessage msg = new FormattedMessage(msgPattern, array);
        formattedMessage = msg.getFormattedMessage();
        throwable = msg.getThrowable();
        return formattedMessage;
    }

    @Override
    public String getFormat() {
        return key;
    }

    @Override
    public Object[] getParameters() {
        if (argArray != null) {
            return argArray;
        }
        return stringArgs;
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Override this to use a ResourceBundle.Control in Java 6
     *
     * @param rbBaseName The base name of the resource bundle, a fully qualified class name.
     * @param resourceBundleLocale The locale to use when formatting the message.
     * @param loop If true the key will be treated as a package or class name and a resource bundle will be located
     *            based on all or part of the package name. If false the key is expected to be the exact bundle id.
     * @return The ResourceBundle.
     */
    protected ResourceBundle getResourceBundle(
            final String rbBaseName, final Locale resourceBundleLocale, final boolean loop) {
        ResourceBundle rb = null;

        if (rbBaseName == null) {
            return null;
        }
        try {
            if (resourceBundleLocale != null) {
                rb = ResourceBundle.getBundle(rbBaseName, resourceBundleLocale);
            } else {
                rb = ResourceBundle.getBundle(rbBaseName);
            }
        } catch (final MissingResourceException ex) {
            if (!loop) {
                logger.debug("Unable to locate ResourceBundle {}", rbBaseName);
                return null;
            }
        }

        String substr = rbBaseName;
        int i;
        while (rb == null && (i = substr.lastIndexOf('.')) > 0) {
            substr = substr.substring(0, i);
            try {
                if (resourceBundleLocale != null) {
                    rb = ResourceBundle.getBundle(substr, resourceBundleLocale);
                } else {
                    rb = ResourceBundle.getBundle(substr);
                }
            } catch (final MissingResourceException ex) {
                logger.debug("Unable to locate ResourceBundle {}", substr);
            }
        }
        return rb;
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        getFormattedMessage();
        out.writeUTF(formattedMessage);
        out.writeUTF(key);
        out.writeUTF(baseName);
        out.writeInt(argArray.length);
        stringArgs = new String[argArray.length];
        int i = 0;
        for (final Object obj : argArray) {
            stringArgs[i] = obj.toString();
            ++i;
        }
        out.writeObject(stringArgs);
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        formattedMessage = in.readUTF();
        key = in.readUTF();
        baseName = in.readUTF();
        in.readInt();
        stringArgs = (String[]) in.readObject();
        logger = StatusLogger.getLogger();
        resourceBundle = null;
        argArray = null;
    }
}
