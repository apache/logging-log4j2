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

import org.apache.logging.log4j.internal.StatusLogger;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 *
 */
public class LocalizedMessage extends ParameterizedMessage {

    private String bundleId;

    private transient ResourceBundle bundle;

    private Locale locale;

    private Map<MessageHint, String> hints = new HashMap<MessageHint, String>();

    private StatusLogger logger = StatusLogger.getLogger();

    public LocalizedMessage() {
        super();
        setup(null, null, null);
    }

    public LocalizedMessage(String messagePattern, String[] stringArgs, Throwable throwable) {
        super(messagePattern, stringArgs, throwable);
        setup(null, null, null);
    }


    public LocalizedMessage(String bundleId, String key, String[] stringArgs,
                            Throwable throwable) {
        super(key, stringArgs, throwable);
        setup(bundleId, null, null);
    }

    public LocalizedMessage(ResourceBundle bundle, String key, String[] stringArgs,
                            Throwable throwable) {
        super(key, stringArgs, throwable);
        setup(null, bundle, null);
    }

    public LocalizedMessage(String bundleId, Locale locale, String key, String[] stringArgs,
                            Throwable throwable) {
        super(key, stringArgs, throwable);
        setup(bundleId, null, locale);
    }

    public LocalizedMessage(ResourceBundle bundle, Locale locale, String key, String[] stringArgs,
                            Throwable throwable) {
        super(key, stringArgs, throwable);
        setup(null, bundle, locale);
    }

    public LocalizedMessage(Locale locale, String key, String[] stringArgs, Throwable throwable) {
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
     * @return a LocalizedMessage containing the messagePattern, converted arguments and,
     * optionally, a Throwable.
     */
    public LocalizedMessage(String messagePattern, Object[] arguments) {
        super(messagePattern, arguments);
        setup(null, null, null);
    }

    public LocalizedMessage(String bundleId, String key, Object[] arguments) {
        super(key, arguments);
        setup(bundleId, null, null);
    }

    public LocalizedMessage(ResourceBundle bundle, String key, Object[] arguments) {
        super(key, arguments);
        setup(null, bundle, null);
    }

    public LocalizedMessage(String bundleId, Locale locale, String key, Object[] arguments) {
        super(key, arguments);
        setup(bundleId, null, locale);
    }

    public LocalizedMessage(ResourceBundle bundle, Locale locale, String key, Object[] arguments) {
        super(key, arguments);
        setup(null, bundle, locale);
    }

    public LocalizedMessage(Locale locale, String key, Object[] arguments) {
        super(key, arguments);
        setup(null, null, locale);
    }

    public LocalizedMessage(String messagePattern, Object arg) {
        super(messagePattern, arg);
        setup(null, null, null);
    }

    public LocalizedMessage(String bundleId, String key, Object arg) {
        super(key, arg);
        setup(bundleId, null, null);
    }

    public LocalizedMessage(ResourceBundle bundle, String key, Object arg) {
        super(key, arg);
        setup(null, bundle, null);
    }

    public LocalizedMessage(String bundleId, Locale locale, String key, Object arg) {
        super(key, arg);
        setup(bundleId, null, locale);
    }

    public LocalizedMessage(ResourceBundle bundle, Locale locale, String key, Object arg) {
        super(key, arg);
        setup(null, bundle, locale);
    }

    public LocalizedMessage(Locale locale, String key, Object arg) {
        super(key, arg);
        setup(null, null, locale);
    }

    public LocalizedMessage(String messagePattern, Object arg1, Object arg2) {
        super(messagePattern, arg1, arg2);
        setup(null, null, null);
    }

    public LocalizedMessage(String bundleId, String key, Object arg1, Object arg2) {
        super(key, arg1, arg2);
        setup(bundleId, null, null);
    }

    public LocalizedMessage(ResourceBundle bundle, String key, Object arg1, Object arg2) {
        super(key, arg1, arg2);
        setup(null, bundle, null);
    }

    public LocalizedMessage(String bundleId, Locale locale, String key, Object arg1, Object arg2) {
        super(key, arg1, arg2);
        setup(bundleId, null, locale);
    }

    public LocalizedMessage(ResourceBundle bundle, Locale locale, String key, Object arg1,
                            Object arg2) {
        super(key, arg1, arg2);
        setup(null, bundle, locale);
    }

    public LocalizedMessage(Locale locale, String key, Object arg1, Object arg2) {
        super(key, arg1, arg2);
        setup(null, null, locale);
    }

    private void setup(String bundleId, ResourceBundle bundle, Locale locale) {
        this.bundleId = bundleId;
        this.bundle = bundle;
        this.locale = locale;
        hints.put(MessageHint.LOGGER_NAME, "");
    }

    @Override
    public Map<MessageHint, String> getHints() {
        return hints;
    }

    @Override
    public String formatMessage(String messagePattern, String[] args) {
        ResourceBundle bundle = this.bundle;
        if (bundle == null) {
            if (bundleId != null) {
                bundle = getBundle(bundleId, locale, false);
            } else {
                String key = hints.get(MessageHint.LOGGER_NAME);
                bundle = getBundle(key, locale, true);
            }
        }
        String msgPattern = (bundle == null || !bundle.containsKey(messagePattern)) ?
            messagePattern : bundle.getString(messagePattern);
        return format(msgPattern, args);
    }

    /**
     * Override this to use a ResourceBundle.Control in Java 6
     * @param key The key to the bundle.
     * @return The ResourceBundle.
     */
    protected ResourceBundle getBundle(String key, Locale locale, boolean loop) {
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
        } catch (MissingResourceException ex) {
            if (!loop) {
                logger.debug("Unable to locate ResourceBundle " + key);
                return null;
            }
        }

        String substr = key;
        int i;
        while (rb == null && (i = substr.lastIndexOf(".")) > 0) {
            substr = substr.substring(0, i);
            try {
                if (locale != null) {
                    rb = ResourceBundle.getBundle(substr, locale);
                } else {
                    rb = ResourceBundle.getBundle(substr);
                }
            } catch (MissingResourceException ex) {
                logger.debug("Unable to locate ResourceBundle " + substr);
            }
        }
        return rb;
    }
}
