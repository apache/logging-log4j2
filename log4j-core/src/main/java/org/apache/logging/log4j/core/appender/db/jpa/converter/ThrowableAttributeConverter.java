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
package org.apache.logging.log4j.core.appender.db.jpa.converter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * A JPA 2.1 attribute converter for {@link Throwable}s in {@link org.apache.logging.log4j.core.LogEvent}s. This
 * converter is capable of converting both to and from {@link String}s.
 */
@Converter(autoApply = false)
public class ThrowableAttributeConverter implements AttributeConverter<Throwable, String> {
    private static final int CAUSED_BY_STRING_LENGTH = 10;

    private static final Field THROWABLE_CAUSE;

    private static final Field THROWABLE_MESSAGE;

    static {
        try {
            THROWABLE_CAUSE = Throwable.class.getDeclaredField("cause");
            THROWABLE_CAUSE.setAccessible(true);
            THROWABLE_MESSAGE = Throwable.class.getDeclaredField("detailMessage");
            THROWABLE_MESSAGE.setAccessible(true);
        } catch (final NoSuchFieldException e) {
            throw new IllegalStateException("Something is wrong with java.lang.Throwable.", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(final Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        final StringBuilder builder = new StringBuilder();
        this.convertThrowable(builder, throwable);
        return builder.toString();
    }

    private void convertThrowable(final StringBuilder builder, final Throwable throwable) {
        builder.append(throwable.toString()).append('\n');
        for (final StackTraceElement element : throwable.getStackTrace()) {
            builder.append("\tat ").append(element).append('\n');
        }
        if (throwable.getCause() != null) {
            builder.append("Caused by ");
            this.convertThrowable(builder, throwable.getCause());
        }
    }

    @Override
    public Throwable convertToEntityAttribute(final String s) {
        if (Strings.isEmpty(s)) {
            return null;
        }

        final List<String> lines = Arrays.asList(s.split("(\n|\r\n)"));
        return this.convertString(lines.listIterator(), false);
    }

    private Throwable convertString(final ListIterator<String> lines, final boolean removeCausedBy) {
        String firstLine = lines.next();
        if (removeCausedBy) {
            firstLine = firstLine.substring(CAUSED_BY_STRING_LENGTH);
        }
        final int colon = firstLine.indexOf(":");
        String throwableClassName;
        String message = null;
        if (colon > 1) {
            throwableClassName = firstLine.substring(0, colon);
            if (firstLine.length() > colon + 1) {
                message = firstLine.substring(colon + 1).trim();
            }
        } else {
            throwableClassName = firstLine;
        }

        final List<StackTraceElement> stackTrace = new ArrayList<>();
        Throwable cause = null;
        while (lines.hasNext()) {
            final String line = lines.next();

            if (line.startsWith("Caused by ")) {
                lines.previous();
                cause = convertString(lines, true);
                break;
            }

            stackTrace.add(
                    StackTraceElementAttributeConverter.convertString(line.trim().substring(3).trim())
            );
        }

        return this.getThrowable(throwableClassName, message, cause,
                stackTrace.toArray(new StackTraceElement[stackTrace.size()]));
    }

    private Throwable getThrowable(final String throwableClassName, final String message, final Throwable cause,
                                   final StackTraceElement[] stackTrace) {
        try {
            @SuppressWarnings("unchecked")
            final Class<Throwable> throwableClass = (Class<Throwable>) LoaderUtil.loadClass(throwableClassName);

            if (!Throwable.class.isAssignableFrom(throwableClass)) {
                return null;
            }

            Throwable throwable;
            if (message != null && cause != null) {
                throwable = this.getThrowable(throwableClass, message, cause);
                if (throwable == null) {
                    throwable = this.getThrowable(throwableClass, cause);
                    if (throwable == null) {
                        throwable = this.getThrowable(throwableClass, message);
                        if (throwable == null) {
                            throwable = this.getThrowable(throwableClass);
                            if (throwable != null) {
                                THROWABLE_MESSAGE.set(throwable, message);
                                THROWABLE_CAUSE.set(throwable, cause);
                            }
                        } else {
                            THROWABLE_CAUSE.set(throwable, cause);
                        }
                    } else {
                        THROWABLE_MESSAGE.set(throwable, message);
                    }
                }
            } else if (cause != null) {
                throwable = this.getThrowable(throwableClass, cause);
                if (throwable == null) {
                    throwable = this.getThrowable(throwableClass);
                    if (throwable != null) {
                        THROWABLE_CAUSE.set(throwable, cause);
                    }
                }
            } else if (message != null) {
                throwable = this.getThrowable(throwableClass, message);
                if (throwable == null) {
                    throwable = this.getThrowable(throwableClass);
                    if (throwable != null) {
                        THROWABLE_MESSAGE.set(throwable, cause);
                    }
                }
            } else {
                throwable = this.getThrowable(throwableClass);
            }

            if (throwable == null) {
                return null;
            }
            throwable.setStackTrace(stackTrace);
            return throwable;
        } catch (final Exception e) {
            return null;
        }
    }

    private Throwable getThrowable(final Class<Throwable> throwableClass, final String message, final Throwable cause) {
        try {
            @SuppressWarnings("unchecked")
            final
            Constructor<Throwable>[] constructors = (Constructor<Throwable>[]) throwableClass.getConstructors();
            for (final Constructor<Throwable> constructor : constructors) {
                final Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 2) {
                    if (String.class == parameterTypes[0] && Throwable.class.isAssignableFrom(parameterTypes[1])) {
                        return constructor.newInstance(message, cause);
                    } else if (String.class == parameterTypes[1] &&
                            Throwable.class.isAssignableFrom(parameterTypes[0])) {
                        return constructor.newInstance(cause, message);
                    }
                }
            }
            return null;
        } catch (final Exception e) {
            return null;
        }
    }

    private Throwable getThrowable(final Class<Throwable> throwableClass, final Throwable cause) {
        try {
            @SuppressWarnings("unchecked")
            final
            Constructor<Throwable>[] constructors = (Constructor<Throwable>[]) throwableClass.getConstructors();
            for (final Constructor<Throwable> constructor : constructors) {
                final Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1 && Throwable.class.isAssignableFrom(parameterTypes[0])) {
                    return constructor.newInstance(cause);
                }
            }
            return null;
        } catch (final Exception e) {
            return null;
        }
    }

    private Throwable getThrowable(final Class<Throwable> throwableClass, final String message) {
        try {
            return throwableClass.getConstructor(String.class).newInstance(message);
        } catch (final Exception e) {
            return null;
        }
    }

    private Throwable getThrowable(final Class<Throwable> throwableClass) {
        try {
            return throwableClass.newInstance();
        } catch (final Exception e) {
            return null;
        }
    }
}
