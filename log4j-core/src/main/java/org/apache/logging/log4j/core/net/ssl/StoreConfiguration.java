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
package org.apache.logging.log4j.core.net.ssl;

import java.util.Arrays;

import org.apache.logging.log4j.status.StatusLogger;

/**
 *
 */
public class StoreConfiguration<T> {
    protected static final StatusLogger LOGGER = StatusLogger.getLogger();

    private String location;
    private char[] password; // TODO get and set in some obfuscated or encrypted format?

    public StoreConfiguration(final String location, final char[] password) {
        this.location = location;
        this.password = password;
    }

    /**
     * Clears the secret fields in this object.
     */
    public void clearSecrets() {
        this.location = null;
        if (password != null) {
            Arrays.fill(password, Character.MIN_VALUE);
            this.password = null;
        }
    }

    /**
     * @deprecated Use StoreConfiguration(String, char[])
     */
    @Deprecated
    public StoreConfiguration(final String location, final String password) {
        this.location = location;
        this.password = password == null ? null : password.toCharArray();
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    /**
     *
     * @deprecated Use getPasswordAsCharArray()
     */
    @Deprecated
    public String getPassword() {
        return String.valueOf(this.password);
    }

    public char[] getPasswordAsCharArray() {
        return this.password;
    }

    public void setPassword(final char[] password) {
        this.password = password;
    }

    /**
     *
     * @deprecated Use getPasswordAsCharArray()
     */
    @Deprecated
    public void setPassword(final String password) {
        this.password = password == null ? null : password.toCharArray();
    }

    /**
     * @throws StoreConfigurationException May be thrown by subclasses
     */
    protected T load() throws StoreConfigurationException {
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + Arrays.hashCode(password);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StoreConfiguration<?> other = (StoreConfiguration<?>) obj;
        if (location == null) {
            if (other.location != null) {
                return false;
            }
        } else if (!location.equals(other.location)) {
            return false;
        }
        if (!Arrays.equals(password, other.password)) {
            return false;
        }
        return true;
    }
}
