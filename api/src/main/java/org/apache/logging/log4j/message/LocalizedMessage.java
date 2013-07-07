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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.logging.log4j.status.StatusLogger;

/**
 * This class is not the recommended way to Localize messages. It is provided to provide some level
 * of compatibility with Log4j 1.x.
 *
 * The recommended way to localize messages is to simply log a message id. Log events should
 * then be recorded without formatting into some kind of data store. The application that is
 * used to read the events and display them to the user should also localize and format the
 * messages for the end user.
 */
public class LocalizedMessage implements Message, LoggerNameAwareMessage {
    private static final long serialVersionUID = 3893703791567290742L;

    private String bundleId;

    private transient ResourceBundle bundle;

    private Locale locale;

    private transient StatusLogger logger = StatusLogger.getLogger();

    private String loggerName;
    private String messagePattern;
    private String[] stringArgs;
    private transient Object[] argArray;
    private String formattedMessage;
    private transient Throwable throwable;

    /**
     * Constructor with message pattern and arguments.
     *
     * @param messagePattern the message pattern that to be checked for placeholders.
     * @param arguments      the argument array to be converted.
     */
    public LocalizedMessage(final String messagePattern, final Object[] arguments) {
        this((ResourceBundle) null, (Locale) null, messagePattern, arguments);
    }

    public LocalizedMessage(final String bundleId, final String key, final Object[] arguments) {
        this(bundleId, (Locale) null, key, arguments);
    }

    public LocalizedMessage(final ResourceBundle bundle, final String key, final Object[] arguments) {
        this(bundle, (Locale) null, key, arguments);
    }

    public LocalizedMessage(final String bundleId, final Locale locale, final String key, final Object[] arguments) {
        this.messagePattern = key;
        this.argArray = arguments;
        this.throwable = null;
        setup(bundleId, null, locale);
    }

    public LocalizedMessage(final ResourceBundle bundle, final Locale locale, final String key,
                            final Object[] arguments) {
        this.messagePattern = key;
        this.argArray = arguments;
        this.throwable = null;
        setup(null, bundle, locale);
    }

    public LocalizedMessage(final Locale locale, final String key, final Object[] arguments) {
        this((ResourceBundle) null, locale, key, arguments);
    }

    public LocalizedMessage(final String messagePattern, final Object arg) {
        this((ResourceBundle) null, (Locale) null, messagePattern, new Object[] {arg});
    }

    public LocalizedMessage(final String bundleId, final String key, final Object arg) {
        this(bundleId, (Locale) null, key, new Object[] {arg});
    }

    public LocalizedMessage(final ResourceBundle bundle, final String key, final Object arg) {
        this(bundle, (Locale) null, key, new Object[] {arg});
    }

    public LocalizedMessage(final String bundleId, final Locale locale, final String key, final Object arg) {
        this(bundleId, locale, key, new Object[] {arg});
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

    public LocalizedMessage(final String bundleId, final String key, final Object arg1, final Object arg2) {
        this(bundleId, (Locale) null, key, new Object[] {arg1, arg2});
    }

    public LocalizedMessage(final ResourceBundle bundle, final String key, final Object arg1, final Object arg2) {
        this(bundle, (Locale) null, key, new Object[] {arg1, arg2});
    }

    public LocalizedMessage(final String bundleId, final Locale locale, final String key, final Object arg1,
                            final Object arg2) {
        this(bundleId, locale, key, new Object[] {arg1, arg2});
    }

    public LocalizedMessage(final ResourceBundle bundle, final Locale locale, final String key, final Object arg1,
                            final Object arg2) {
        this(bundle, locale, key, new Object[] {arg1, arg2});
    }

    public LocalizedMessage(final Locale locale, final String key, final Object arg1, final Object arg2) {
        this((ResourceBundle) null, locale, key, new Object[] {arg1, arg2});
    }

    /**
     * Set the name of the Logger.
     * @param name The name of the Logger.
     */
    @Override
    public void setLoggerName(final String name) {
        this.loggerName = name;
    }

    /**
     * Returns the name of the Logger.
     * @return the name of the Logger.
     */
    @Override
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
     * @return The formatted message String.
     */
    @Override
    public String getFormattedMessage() {
        if (formattedMessage != null) {
            return formattedMessage;
        }
        ResourceBundle bundle = this.bundle;
        if (bundle == null) {
            if (bundleId != null) {
                bundle = getBundle(bundleId, locale, false);
            } else {
                bundle = getBundle(loggerName, locale, true);
            }
        }
        final String messagePattern = getFormat();
        final String msgPattern = (bundle == null || !bundle.containsKey(messagePattern)) ?
            messagePattern : bundle.getString(messagePattern);
        final Object[] array = argArray == null ? stringArgs : argArray;
        final FormattedMessage msg = new FormattedMessage(msgPattern, array);
        formattedMessage = msg.getFormattedMessage();
        throwable = msg.getThrowable();
        return formattedMessage;
    }

    @Override
    public String getFormat() {
        return messagePattern;
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

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        getFormattedMessage();
        out.writeUTF(formattedMessage);
        out.writeUTF(messagePattern);
        out.writeUTF(bundleId);
        out.writeInt(argArray.length);
        stringArgs = new String[argArray.length];
        int i = 0;
        for (final Object obj : argArray) {
            stringArgs[i] = obj.toString();
            ++i;
        }
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        formattedMessage = in.readUTF();
        messagePattern = in.readUTF();
        bundleId = in.readUTF();
        final int length = in.readInt();
        stringArgs = new String[length];
        for (int i = 0; i < length; ++i) {
            stringArgs[i] = in.readUTF();
        }
        logger = StatusLogger.getLogger();
        bundle = null;
        argArray = null;
    }
}
