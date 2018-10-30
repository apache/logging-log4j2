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

package org.apache.logging.log4j.core.util;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Helper class for closing JNDI resources.
 * 
 * This class is separate from {@link Closer} because JNDI is not in Android.
 */
public final class JndiCloser {
    
    private JndiCloser() {
    }

    /**
     * Closes the specified {@code Context}.
     *
     * @param context the JNDI Context to close, may be {@code null}
     * @throws NamingException if a problem occurred closing the specified JNDI Context
     */
    public static void close(final Context context) throws NamingException {
        if (context != null) {
            context.close();
        }
    }

    /**
     * Closes the specified {@code Context}, ignoring any exceptions thrown by the close operation.
     *
     * @param context the JNDI Context to close, may be {@code null}
     */
    public static boolean closeSilently(final Context context) {
        try {
            close(context);
            return true;
        } catch (final NamingException ignored) {
            // ignored
            return false;
        }
    }

}
