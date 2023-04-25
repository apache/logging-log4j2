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

/**
 * PasswordProvider implementations are able to produce a password from somewhere. The source of the password data
 * is implementation-specific.
 * <p>The {@link #getPassword()} method may be called multiple times as needed, so the
 * caller does not need to (and <b>should not</b>) keep the password data in memory for longer than absolutely
 * necessary. Users of this class should erase the password array by calling
 * {@link java.util.Arrays#fill(char[], char)} immediately when authentication is complete and the password data
 * is no longer needed.
 * </p>
 */
public interface PasswordProvider {

    /**
     * Returns a new char[] array with the password characters.
     * <p>
     * It is the responsibility of the caller to erase this data by calling
     * {@link java.util.Arrays#fill(char[], char)} immediately when authentication is complete and the password data
     * is no longer needed.
     * </p>
     * @return a copy of the password
     */
    char[] getPassword();
}
