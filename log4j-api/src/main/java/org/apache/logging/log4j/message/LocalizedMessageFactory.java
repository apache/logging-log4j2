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

import java.util.Objects;
import java.util.ResourceBundle;
import org.jspecify.annotations.Nullable;

/**
 * Creates {@link FormattedMessage} instances for {@link MessageFactory2} methods (and {@link MessageFactory} by
 * extension.)
 *
 * <p>
 * <strong>Note to implementors:</strong>
 * </p>
 * <p>
 * This class does <em>not</em> implement any {@link MessageFactory2} methods and lets the superclass funnel those calls
 * through {@link #newMessage(String, Object...)}.
 * </p>
 */
public class LocalizedMessageFactory extends AbstractMessageFactory {
    private static final long serialVersionUID = -1996295808703146741L;

    @Nullable
    // FIXME: cannot use ResourceBundle name for serialization until Java 8
    private final transient ResourceBundle resourceBundle;

    @Nullable
    private final String baseName;

    public LocalizedMessageFactory(final ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.baseName = null;
    }

    public LocalizedMessageFactory(final String baseName) {
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
     * @since 2.8
     */
    @Override
    public Message newMessage(final String key) {
        if (resourceBundle == null) {
            return new LocalizedMessage(baseName, key);
        }
        return new LocalizedMessage(resourceBundle, key);
    }

    /**
     * Creates {@link LocalizedMessage} instances.
     *
     * @param key The key String, used as a message if the key is absent.
     * @param params The parameters for the message at the given key.
     * @return The LocalizedMessage.
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(String, Object...)
     */
    @Override
    public Message newMessage(final String key, final Object... params) {
        if (resourceBundle == null) {
            return new LocalizedMessage(baseName, key, params);
        }
        return new LocalizedMessage(resourceBundle, key, params);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final LocalizedMessageFactory that = (LocalizedMessageFactory) object;
        return Objects.equals(resourceBundle, that.resourceBundle) && Objects.equals(baseName, that.baseName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceBundle, baseName);
    }
}
