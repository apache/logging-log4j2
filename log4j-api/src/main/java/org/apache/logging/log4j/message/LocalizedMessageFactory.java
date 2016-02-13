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

import java.util.ResourceBundle;

/**
 * Creates {@link org.apache.logging.log4j.message.LocalizedMessage} instances for
 * {@link #newMessage(String, Object...)}.
 */
public class LocalizedMessageFactory extends AbstractMessageFactory {

    private static final long serialVersionUID = 1L;

    // FIXME: cannot use ResourceBundle name for serialization until Java 8
    private transient final ResourceBundle resourceBundle;
    private final String baseName;

    public LocalizedMessageFactory(final ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.baseName = null;
    }

    /**
     * Constructs a message factory with the given entry and exit strings.
     * @param entryText the text to use for trace entry, like {@code "entry"} or {@code "Enter"}.
     * @param exitText the text to use for trace exit, like {@code "exit"} or {@code "Exit"}.
     * @since 2.6
     */
    public LocalizedMessageFactory(final ResourceBundle resourceBundle, final String entryText, final String exitText) {
        super(entryText, exitText);
        this.resourceBundle = resourceBundle;
        this.baseName = null;
    }

    public LocalizedMessageFactory(final String baseName) {
        this.resourceBundle = null;
        this.baseName = baseName;
    }

    /**
     * Constructs a message factory with the given entry and exit strings.
     * @param entryText the text to use for trace entry, like {@code "entry"} or {@code "Enter"}.
     * @param exitText the text to use for trace exit, like {@code "exit"} or {@code "Exit"}.
     * @since 2.6
     */
    public LocalizedMessageFactory(final String baseName, final String entryText, final String exitText) {
        super(entryText, exitText);
        this.resourceBundle = null;
        this.baseName = baseName;
    }

    /**
     * Gets the resource bundle base name if set.
     * 
     * @return the resource bundle base name if set. May be null.
     */
    public String getBaseName() {
        return this.baseName;
    }

    /**
     * Gets the resource bundle if set.
     * 
     * @return the resource bundle if set. May be null.
     */
    public ResourceBundle getResourceBundle() {
        return this.resourceBundle;
    }

    /**
     * Creates {@link org.apache.logging.log4j.message.StringFormattedMessage} instances.
     *
     * @param key The key String, used as a message if the key is absent.
     * @param params The parameters for the message at the given key.
     * @return The Message.
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(String, Object...)
     */
    @Override
    public Message newMessage(final String key, final Object... params) {
        if (resourceBundle == null) {
            return new LocalizedMessage(baseName,  key, params);
        }
        return new LocalizedMessage(resourceBundle, key, params);
    }
}
