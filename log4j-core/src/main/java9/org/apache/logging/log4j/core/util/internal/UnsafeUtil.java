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

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import sun.misc.Unsafe;

/**
 * Provides access to unsafe operations.
 */
public class UnsafeUtil {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Unsafe unsafe = findUnsafe();

    private static Unsafe findUnsafe() {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Unsafe>() {

                @Override
                public Unsafe run() throws ReflectiveOperationException, SecurityException {
                    final Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
                    unsafeField.setAccessible(true);
                    return (Unsafe) unsafeField.get(null);
                }
            });
        } catch (PrivilegedActionException e) {
            final Exception wrapped = e.getException();
            if (wrapped instanceof SecurityException) {
                throw (SecurityException) wrapped;
            }
            LOGGER.warn("sun.misc.Unsafe is not available. This will impact memory usage.", e);
        }
        return null;
    }

    public static void clean(final ByteBuffer bb) throws Exception {
        if (unsafe != null && bb.isDirect()) {
            unsafe.invokeCleaner(bb);
        }
    }
}
