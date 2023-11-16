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
package org.apache.logging.log4j.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for working with Java {@link Type}s and derivatives. This class is adapted heavily from the
 * <a href="http://projects.spring.io/spring-framework/">Spring Framework</a>, specifically the
 * <a href="http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/util/TypeUtils.html">TypeUtils</a>
 * class.
 *
 * @see java.lang.reflect.Type
 * @see java.lang.reflect.GenericArrayType
 * @see java.lang.reflect.ParameterizedType
 * @see java.lang.reflect.WildcardType
 * @see java.lang.Class
 * @since 2.1
 */
public final class TypeUtil {

    private TypeUtil() {}

    /**
     * Gets all declared fields for the given class (including superclasses).
     *
     * @param cls the class to examine
     * @return all declared fields for the given class (including superclasses).
     * @see Class#getDeclaredFields()
     */
    public static List<Field> getAllDeclaredFields(Class<?> cls) {
        final List<Field> fields = new ArrayList<>();
        while (cls != null) {
            Collections.addAll(fields, cls.getDeclaredFields());
            cls = cls.getSuperclass();
        }
        return fields;
    }
    /**
     * Indicates if two {@link Type}s are assignment compatible.
     *
     * @param lhs the left hand side to check assignability to
     * @param rhs the right hand side to check assignability from
     * @return {@code true} if it is legal to assign a variable of type {@code rhs} to a variable of type {@code lhs}
     * @see Class#isAssignableFrom(Class)
     */
    public static boolean isAssignable(final Type lhs, final Type rhs) {
        Objects.requireNonNull(lhs, "No left hand side type provided");
        Objects.requireNonNull(rhs, "No right hand side type provided");
        if (lhs.equals(rhs)) {
            return true;
        }
        if (Object.class.equals(lhs)) {
            // everything is assignable to Object
            return true;
        }
        // raw type on left
        if (lhs instanceof Class<?>) {
            final Class<?> lhsClass = (Class<?>) lhs;
            if (rhs instanceof Class<?>) {
                // no generics involved
                final Class<?> rhsClass = (Class<?>) rhs;
                return lhsClass.isAssignableFrom(rhsClass);
            }
            if (rhs instanceof ParameterizedType) {
                // check to see if the parameterized type has the same raw type as the lhs; this is legal
                final Type rhsRawType = ((ParameterizedType) rhs).getRawType();
                if (rhsRawType instanceof Class<?>) {
                    return lhsClass.isAssignableFrom((Class<?>) rhsRawType);
                }
            }
            if (lhsClass.isArray() && rhs instanceof GenericArrayType) {
                // check for compatible array component types
                return isAssignable(lhsClass.getComponentType(), ((GenericArrayType) rhs).getGenericComponentType());
            }
        }
        // parameterized type on left
        if (lhs instanceof ParameterizedType) {
            final ParameterizedType lhsType = (ParameterizedType) lhs;
            if (rhs instanceof Class<?>) {
                final Type lhsRawType = lhsType.getRawType();
                if (lhsRawType instanceof Class<?>) {
                    return ((Class<?>) lhsRawType).isAssignableFrom((Class<?>) rhs);
                }
            } else if (rhs instanceof ParameterizedType) {
                final ParameterizedType rhsType = (ParameterizedType) rhs;
                return isParameterizedAssignable(lhsType, rhsType);
            }
        }
        // generic array type on left
        if (lhs instanceof GenericArrayType) {
            final Type lhsComponentType = ((GenericArrayType) lhs).getGenericComponentType();
            if (rhs instanceof Class<?>) {
                // raw type on right
                final Class<?> rhsClass = (Class<?>) rhs;
                if (rhsClass.isArray()) {
                    return isAssignable(lhsComponentType, rhsClass.getComponentType());
                }
            } else if (rhs instanceof GenericArrayType) {
                return isAssignable(lhsComponentType, ((GenericArrayType) rhs).getGenericComponentType());
            }
        }
        // wildcard type on left
        if (lhs instanceof WildcardType) {
            return isWildcardAssignable((WildcardType) lhs, rhs);
        }
        // strange...
        return false;
    }

    private static boolean isParameterizedAssignable(final ParameterizedType lhs, final ParameterizedType rhs) {
        if (lhs.equals(rhs)) {
            // that was easy
            return true;
        }
        final Type[] lhsTypeArguments = lhs.getActualTypeArguments();
        final Type[] rhsTypeArguments = rhs.getActualTypeArguments();
        final int size = lhsTypeArguments.length;
        if (rhsTypeArguments.length != size) {
            // clearly incompatible types
            return false;
        }
        for (int i = 0; i < size; i++) {
            // verify all type arguments are assignable
            final Type lhsArgument = lhsTypeArguments[i];
            final Type rhsArgument = rhsTypeArguments[i];
            if (!lhsArgument.equals(rhsArgument)
                    && !(lhsArgument instanceof WildcardType
                            && isWildcardAssignable((WildcardType) lhsArgument, rhsArgument))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWildcardAssignable(final WildcardType lhs, final Type rhs) {
        final Type[] lhsUpperBounds = getEffectiveUpperBounds(lhs);
        final Type[] lhsLowerBounds = getEffectiveLowerBounds(lhs);
        if (rhs instanceof WildcardType) {
            // oh boy, this scenario requires checking a lot of assignability!
            final WildcardType rhsType = (WildcardType) rhs;
            final Type[] rhsUpperBounds = getEffectiveUpperBounds(rhsType);
            final Type[] rhsLowerBounds = getEffectiveLowerBounds(rhsType);
            for (final Type lhsUpperBound : lhsUpperBounds) {
                for (final Type rhsUpperBound : rhsUpperBounds) {
                    if (!isBoundAssignable(lhsUpperBound, rhsUpperBound)) {
                        return false;
                    }
                }
                for (final Type rhsLowerBound : rhsLowerBounds) {
                    if (!isBoundAssignable(lhsUpperBound, rhsLowerBound)) {
                        return false;
                    }
                }
            }
            for (final Type lhsLowerBound : lhsLowerBounds) {
                for (final Type rhsUpperBound : rhsUpperBounds) {
                    if (!isBoundAssignable(rhsUpperBound, lhsLowerBound)) {
                        return false;
                    }
                }
                for (final Type rhsLowerBound : rhsLowerBounds) {
                    if (!isBoundAssignable(rhsLowerBound, lhsLowerBound)) {
                        return false;
                    }
                }
            }
        } else {
            // phew, far less bounds to check
            for (final Type lhsUpperBound : lhsUpperBounds) {
                if (!isBoundAssignable(lhsUpperBound, rhs)) {
                    return false;
                }
            }
            for (final Type lhsLowerBound : lhsLowerBounds) {
                if (!isBoundAssignable(lhsLowerBound, rhs)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Type[] getEffectiveUpperBounds(final WildcardType type) {
        final Type[] upperBounds = type.getUpperBounds();
        return upperBounds.length == 0 ? new Type[] {Object.class} : upperBounds;
    }

    private static Type[] getEffectiveLowerBounds(final WildcardType type) {
        final Type[] lowerBounds = type.getLowerBounds();
        return lowerBounds.length == 0 ? new Type[] {null} : lowerBounds;
    }

    private static boolean isBoundAssignable(final Type lhs, final Type rhs) {
        return (rhs == null) || ((lhs != null) && isAssignable(lhs, rhs));
    }
}
