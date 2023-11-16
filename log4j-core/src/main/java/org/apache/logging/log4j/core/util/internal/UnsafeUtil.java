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
package org.apache.logging.log4j.core.util.internal;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Provides access to unsafe operations.
 */
public class UnsafeUtil {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static Method cleanerMethod;
    private static Method cleanMethod;

    static {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

                @Override
                public Void run() throws ReflectiveOperationException, SecurityException {
                    final ByteBuffer direct = ByteBuffer.allocateDirect(1);
                    cleanerMethod = direct.getClass().getDeclaredMethod("cleaner");
                    cleanerMethod.setAccessible(true);
                    final Object cleaner = cleanerMethod.invoke(direct);
                    cleanMethod = cleaner.getClass().getMethod("clean");
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            final Exception wrapped = e.getException();
            if (wrapped instanceof SecurityException) {
                throw (SecurityException) wrapped;
            }
            LOGGER.warn("sun.misc.Cleaner#clean() is not accessible. This will impact memory usage.", wrapped);
            cleanerMethod = null;
            cleanMethod = null;
        }
    }

    public static void clean(final ByteBuffer bb) throws Exception {
        if (cleanerMethod != null && cleanMethod != null && bb.isDirect()) {
            cleanMethod.invoke(cleanerMethod.invoke(bb));
        }
    }
}
