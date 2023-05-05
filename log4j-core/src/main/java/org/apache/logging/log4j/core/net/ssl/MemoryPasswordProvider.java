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

/**
 * Simple PasswordProvider implementation that keeps the password char[] array in memory.
 * <p>
 * This implementation is not very secure because the password data is resident in memory during the life of this
 * provider object, giving attackers a large window of opportunity to obtain the password from a memory dump.
 * A slightly more secure implementation is {@link EnvironmentPasswordProvider},
 * and an even more secure implementation is {@link FilePasswordProvider}.
 * </p>
 */
class MemoryPasswordProvider implements PasswordProvider {
    private final char[] password;

    public MemoryPasswordProvider(final char[] chars) {
        if (chars != null) {
            password = chars.clone();
        } else {
            password = null;
        }
    }

    @Override
    public char[] getPassword() {
        if (password == null) {
            return null;
        }
        return password.clone();
    }

    public void clearSecrets() {
        Arrays.fill(password, '\0');
    }
}
