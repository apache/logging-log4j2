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

import java.util.Objects;

/**
 * PasswordProvider implementation that obtains the password value from a system environment variable.
 * <p>
 * This implementation is not very secure because the Java interface to obtain system environment variable values
 * requires us to use String objects. String objects are immutable and Java does not provide a way to erase this
 * sensitive data from the application memory. The password data will stay resident in memory until the String object
 * and its associated char[] array object are garbage collected and the memory is overwritten by another object.
 * </p><p>
 * This is slightly more secure than {@link MemoryPasswordProvider} because the actual password string does not
 * need to be passed to the application.
 * The actual password string is not pulled into memory until it is needed
 * (so the password string does not need to be passed in from the command line or in a configuration file).
 * This gives an attacker a smaller window  of opportunity to obtain the password from a memory dump.
 * </p><p>
 * A more secure implementation is {@link FilePasswordProvider}.
 * </p>
 */
class EnvironmentPasswordProvider implements PasswordProvider {
    private final String passwordEnvironmentVariable;

    /**
     * Constructs a new EnvironmentPasswordProvider with the specified environment variable name
     * @param passwordEnvironmentVariable name of the system environment variable that holds the password
     */
    public EnvironmentPasswordProvider(final String passwordEnvironmentVariable) {
        this.passwordEnvironmentVariable =
                Objects.requireNonNull(passwordEnvironmentVariable, "passwordEnvironmentVariable");
    }

    @Override
    public char[] getPassword() {
        final String password = System.getenv(passwordEnvironmentVariable);
        return password == null ? null : password.toCharArray();
    }
}
