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
package org.apache.log4j.spi;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.log4j.Category;
import org.apache.logging.log4j.core.util.Throwables;

/**
 * Log4j's internal representation of throwables.
 */
public class ThrowableInformation implements java.io.Serializable {

    static final long serialVersionUID = -4748765566864322735L;

    private transient Throwable throwable;
    private transient Category category;
    private String[] rep;
    private Method toStringList;

    /**
     * Constructs new instance.
     *
     * @since 1.2.15
     * @param r String representation of throwable.
     */
    public ThrowableInformation(final String[] r) {
        this.rep = rep != null ? r.clone() : null;
    }

    /**
     * Constructs new instance.
     */
    public ThrowableInformation(Throwable throwable) {
        this.throwable = throwable;
        Method method = null;
        try {
            final Class<?> throwables = Class.forName("org.apache.logging.log4j.core.util.Throwables");
            method = throwables.getMethod("toStringList", Throwable.class);
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            // Ignore the exception if Log4j-core is not present.
        }
        this.toStringList = method;
    }

    /**
     * Constructs a new instance.
     *
     * @param throwable throwable, may not be null.
     * @param category category used to obtain ThrowableRenderer, may be null.
     * @since 1.2.16
     */
    public ThrowableInformation(final Throwable throwable, final Category category) {
        this(throwable);
        this.category = category;
        this.rep = null;
    }

    public
    Throwable getThrowable() {
        return throwable;
    }

    public synchronized String[] getThrowableStrRep() {
        if (toStringList != null && throwable != null) {
            try {
                @SuppressWarnings("unchecked")
                final
                List<String> elements = (List<String>) toStringList.invoke(null, throwable);
                if (elements != null) {
                    return elements.toArray(new String[0]);
                }
            } catch (final ReflectiveOperationException ex) {
                // Ignore the exception.
            }
        }
        return rep;
    }
}
