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
package org.apache.logging.log4j.core.net.ssl;

import java.util.Arrays;
import java.util.Objects;
import org.apache.logging.log4j.status.StatusLogger;

/**
 *
 */
public class StoreConfiguration<T> {

    static final String PKCS12 = "PKCS12";
    static final String JKS = "JKS";
    protected static final StatusLogger LOGGER = StatusLogger.getLogger();

    private String location;
    private PasswordProvider passwordProvider;

    public StoreConfiguration(final String location, final PasswordProvider passwordProvider) {
        this.location = location;
        this.passwordProvider = Objects.requireNonNull(passwordProvider, "passwordProvider");
    }

    /**
     * @deprecated Use {@link #StoreConfiguration(String, PasswordProvider)}
     */
    @Deprecated
    public StoreConfiguration(final String location, final char[] password) {
        this(location, new MemoryPasswordProvider(password));
    }

    /**
     * @deprecated Use {@link #StoreConfiguration(String, PasswordProvider)}
     */
    @Deprecated
    public StoreConfiguration(final String location, final String password) {
        this(location, new MemoryPasswordProvider(password == null ? null : password.toCharArray()));
    }

    /**
     * Clears the secret fields in this object.
     */
    public void clearSecrets() {
        this.location = null;
        this.passwordProvider = null;
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
        return String.valueOf(this.passwordProvider.getPassword());
    }

    public char[] getPasswordAsCharArray() {
        return this.passwordProvider.getPassword();
    }

    public void setPassword(final char[] password) {
        this.passwordProvider = new MemoryPasswordProvider(password);
    }

    /**
     *
     * @deprecated Use getPasswordAsCharArray()
     */
    @Deprecated
    public void setPassword(final String password) {
        this.passwordProvider = new MemoryPasswordProvider(password == null ? null : password.toCharArray());
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
        result = prime * result + Arrays.hashCode(passwordProvider.getPassword());
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
        if (!Objects.equals(location, other.location)) {
            return false;
        }
        if (!Arrays.equals(passwordProvider.getPassword(), other.passwordProvider.getPassword())) {
            return false;
        }
        return true;
    }
}
