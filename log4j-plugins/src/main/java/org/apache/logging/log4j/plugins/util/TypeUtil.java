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
package org.apache.logging.log4j.plugins.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for working with Java {@link Type}s and derivatives. This class is adapted heavily from the
 * <a href="http://projects.spring.io/spring-framework/">Spring Framework</a>, specifically the
 * <a href="http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/util/TypeUtils.html">TypeUtils</a>
 * class.
 *
 * @see Type
 * @see GenericArrayType
 * @see ParameterizedType
 * @see WildcardType
 * @see Class
 * @since 2.1
 */
public final class TypeUtil {

    private TypeUtil() {
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
                // possible for primitive types here
                return isAssignable(lhsClass, rhsClass);
            }
            if (rhs instanceof ParameterizedType) {
                return isAssignable(lhsClass, getRawType(rhs));
            } else if (lhsClass.isArray() && rhs instanceof GenericArrayType) {
                // check for compatible array component types
                return isAssignable(lhsClass.getComponentType(), ((GenericArrayType) rhs).getGenericComponentType());
            } else if (rhs instanceof TypeVariable<?>) {
                for (final Type bound : ((TypeVariable<?>) rhs).getBounds()) {
                    if (isAssignable(lhs, bound)) {
                        return true;
                    }
                }
            } else {
                return false;
            }
        }
        // parameterized type on left
        if (lhs instanceof ParameterizedType) {
            final ParameterizedType lhsType = (ParameterizedType) lhs;
            if (rhs instanceof Class<?>) {
                return isAssignable(getRawType(lhs), (Class<?>) rhs);
            } else if (rhs instanceof ParameterizedType) {
                final ParameterizedType rhsType = (ParameterizedType) rhs;
                return isParameterizedAssignable(lhsType, rhsType);
            } else if (rhs instanceof TypeVariable<?>) {
                for (final Type bound : ((TypeVariable<?>) rhs).getBounds()) {
                    if (isAssignable(lhsType, bound)) {
                        return true;
                    }
                }
            } else {
                return false;
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

    private static boolean isAssignable(final Class<?> lhs, final Class<?> rhs) {
        return getReferenceType(lhs).isAssignableFrom(getReferenceType(rhs));
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
            if (!lhsArgument.equals(rhsArgument) &&
                    !(lhsArgument instanceof WildcardType &&
                            isWildcardAssignable((WildcardType) lhsArgument, rhsArgument))) {
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
        return upperBounds.length == 0 ? new Type[]{Object.class} : upperBounds;
    }

    private static Type[] getEffectiveLowerBounds(final WildcardType type) {
        final Type[] lowerBounds = type.getLowerBounds();
        return lowerBounds.length == 0 ? new Type[]{null} : lowerBounds;
    }

    private static boolean isBoundAssignable(final Type lhs, final Type rhs) {
        return (rhs == null) || ((lhs != null) && isAssignable(lhs, rhs));
    }

    /**
     * Extracts the raw type equivalent of a given type.
     */
    public static Class<?> getRawType(final Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return getRawType(((ParameterizedType) type).getRawType());
        }
        if (type instanceof GenericArrayType) {
            return Array.newInstance(getRawType(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
        }
        if (type instanceof WildcardType) {
            final Type[] bounds = ((WildcardType) type).getUpperBounds();
            return bounds.length > 0 ? getRawType(bounds[0]) : Object.class;
        }
        if (type instanceof TypeVariable<?>) {
            final Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length > 0 ? getRawType(bounds[0]) : Object.class;
        }
        return Object.class;
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_BOXED_TYPES = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            double.class, Double.class,
            float.class, Float.class,
            int.class, Integer.class,
            long.class, Long.class,
            short.class, Short.class);

    /**
     * Returns the reference type for a class. For primitives, this is their boxed equivalent. For other types, this is
     * the class unchanged.
     */
    public static Class<?> getReferenceType(final Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return PRIMITIVE_BOXED_TYPES.get(clazz);
        }
        return clazz;
    }

    public static Type getSuperclassTypeParameter(final Class<?> type) {
        final Type genericSuperclass = type.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            return ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
        }
        throw new IllegalArgumentException(type + " does not have type parameters");
    }

    public static boolean isEqual(final Type first, final Type second) {
        if (first == second) {
            return true;
        }
        if (first instanceof Class<?>) {
            return first.equals(second);
        }
        if (first instanceof ParameterizedType) {
            if (!(second instanceof ParameterizedType)) {
                return false;
            }
            final var firstType = (ParameterizedType) first;
            final var secondType = (ParameterizedType) second;
            return Objects.equals(firstType.getOwnerType(), secondType.getOwnerType()) &&
                    firstType.getRawType().equals(secondType.getRawType()) &&
                    Arrays.equals(firstType.getActualTypeArguments(), secondType.getActualTypeArguments());
        }
        if (first instanceof GenericArrayType) {
            if (!(second instanceof GenericArrayType)) {
                return false;
            }
            return isEqual(((GenericArrayType) first).getGenericComponentType(),
                    ((GenericArrayType) second).getGenericComponentType());
        }
        if (first instanceof WildcardType) {
            if (!(second instanceof WildcardType)) {
                return false;
            }
            final var firstType = (WildcardType) first;
            final var secondType = (WildcardType) second;
            return Arrays.equals(firstType.getUpperBounds(), secondType.getUpperBounds()) &&
                    Arrays.equals(firstType.getLowerBounds(), secondType.getLowerBounds());
        }
        if (first instanceof TypeVariable<?>) {
            if (!(second instanceof TypeVariable<?>)) {
                return false;
            }
            final var firstType = (TypeVariable<?>) first;
            final var secondType = (TypeVariable<?>) second;
            return firstType.getName().equals(secondType.getName()) &&
                    firstType.getGenericDeclaration().equals(secondType.getGenericDeclaration());
        }
        return false;
    }

}
