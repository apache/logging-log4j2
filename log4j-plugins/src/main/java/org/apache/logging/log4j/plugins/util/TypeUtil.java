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
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
     * Gets all declared fields for the given class (including superclasses).
     *
     * @param cls the class to examine
     * @return all declared fields for the given class (including superclasses).
     * @see Class#getDeclaredFields()
     */
    public static List<Field> getAllDeclaredFields(Class<?> cls) {
        final List<Field> fields = new ArrayList<>();
        while (cls != null) {
            final Field[] declaredFields = cls.getDeclaredFields();
            fields.addAll(Arrays.asList(declaredFields));
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
     * Checks if a type is a raw type.
     */
    public static boolean isRawType(final Type type) {
        if (type instanceof Class<?>) {
            final Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return isRawType(clazz.getComponentType());
            }
            return clazz.getTypeParameters().length > 0;
        }
        return false;
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

    /**
     * Checks if a type matches another type.
     */
    public static boolean typesMatch(final Type required, final Type found) {
        if (required instanceof Class<?>) {
            if (found instanceof Class<?>) {
                return required.equals(found);
            }
            if (found instanceof ParameterizedType) {
                return required.equals(getRawType(found));
            }
        }
        if (required instanceof ParameterizedType) {
            if (found instanceof Class<?>) {
                return getRawType(required).equals(found);
            }
            if (found instanceof ParameterizedType) {
                if (!getRawType(required).equals(getRawType(found))) {
                    return false;
                }
                final Type[] requiredArguments = ((ParameterizedType) required).getActualTypeArguments();
                final Type[] foundArguments = ((ParameterizedType) found).getActualTypeArguments();
                if (requiredArguments.length != foundArguments.length) {
                    return false;
                }
                for (int i = 0; i < requiredArguments.length; i++) {
                    if (!typeParametersMatch(requiredArguments[i], foundArguments[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static boolean typeParametersMatch(final Type required, final Type found) {
        if (required instanceof Class<?> || required instanceof ParameterizedType || required instanceof GenericArrayType) {
            if (found instanceof Class<?> || found instanceof ParameterizedType || found instanceof GenericArrayType) {
                return typesMatch(getReferenceType(required), getReferenceType(found));
            }
            if (found instanceof TypeVariable<?>) {
                return typeParametersMatch(required, (TypeVariable<?>) found);
            }
        }
        if (required instanceof WildcardType) {
            final WildcardType wildcardType = (WildcardType) required;
            if (found instanceof Class<?> || found instanceof ParameterizedType || found instanceof GenericArrayType) {
                return typeParametersMatch(wildcardType, found);
            }
            if (found instanceof TypeVariable<?>) {
                return typeParametersMatch(wildcardType, (TypeVariable<?>) found);
            }
        }
        if (required instanceof TypeVariable<?>) {
            if (found instanceof TypeVariable<?>) {
                final Type[] foundBounds = getTopBounds(((TypeVariable<?>) found).getBounds());
                final Type[] requiredBounds = getTopBounds(((TypeVariable<?>) required).getBounds());
                return areBoundsStricter(foundBounds, requiredBounds);
            }
        }
        return false;
    }

    private static boolean typeParametersMatch(final Type required, final TypeVariable<?> found) {
        for (final Type bound : getTopBounds(found.getBounds())) {
            if (!isAssignable(bound, required)) {
                return false;
            }
        }
        return true;
    }

    private static boolean typeParametersMatch(final WildcardType required, final Type found) {
        return lowerBoundsOfWildcardMatch(required, found) && upperBoundsOfWildcardMatch(required, found);
    }

    private static boolean typeParametersMatch(final WildcardType required, final TypeVariable<?> found) {
        final Type[] bounds = getTopBounds(found.getBounds());
        if (!lowerBoundsOfWildcardMatch(required, bounds)) {
            return false;
        }
        final Type[] upperBounds = required.getUpperBounds();
        return areBoundsStricter(bounds, upperBounds) || areBoundsStricter(upperBounds, bounds);
    }

    private static boolean lowerBoundsOfWildcardMatch(final WildcardType required, final Type... found) {
        final Type[] lowerBounds = required.getLowerBounds();
        return lowerBounds.length == 0 || areBoundsStricter(found, lowerBounds);
    }

    private static boolean upperBoundsOfWildcardMatch(final WildcardType required, final Type... found) {
        return areBoundsStricter(required.getUpperBounds(), found);
    }

    private static boolean areBoundsStricter(final Type[] upperBounds, final Type[] stricterUpperBounds) {
        final Type[] stricterBounds = getTopBounds(stricterUpperBounds);
        for (final Type upperBound : getTopBounds(upperBounds)) {
            if (!isAssignableFromOneOf(upperBound, stricterBounds)) {
                return false;
            }
        }
        return true;
    }

    private static Type[] getTopBounds(final Type[] bounds) {
        return bounds[0] instanceof TypeVariable<?> ? getTopBounds(((TypeVariable<?>) bounds[0]).getBounds()) : bounds;
    }

    private static boolean isAssignableFromOneOf(final Type type, final Type... types) {
        for (final Type t : types) {
            if (isAssignable(type, t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the type closure of a generic type.
     */
    public static Collection<Type> getTypeClosure(final Type type) {
        return TYPE_CLOSURE_CACHE.get(type);
    }

    private static final Cache<Type, Collection<Type>> TYPE_CLOSURE_CACHE =
            WeakCache.newWeakRefCache(type -> new TypeResolver(type).types.values());

    private static class TypeResolver {
        private final Map<TypeVariable<?>, Type> resolvedTypeVariables = new HashMap<>();
        private final Map<Class<?>, Type> types = new LinkedHashMap<>();

        private TypeResolver(final Type type) {
            loadTypes(type);
        }

        private void loadTypes(final Type type) {
            if (type instanceof Class<?>) {
                final Class<?> clazz = (Class<?>) type;
                types.put(clazz, clazz);
                loadTypes(clazz);
            } else if (isRawType(type)) {
                loadTypes(getRawType(type));
            } else if (type instanceof GenericArrayType) {
                final GenericArrayType arrayType = (GenericArrayType) type;
                final Type componentType = arrayType.getGenericComponentType();
                final Class<?> rawComponentType = getRawType(componentType);
                final Class<?> arrayClass = Array.newInstance(rawComponentType, 0).getClass();
                types.put(arrayClass, arrayType);
                loadTypes(arrayClass);
            } else if (type instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType) type;
                final Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class<?>) {
                    final Class<?> clazz = (Class<?>) rawType;
                    processTypeVariables(clazz.getTypeParameters(), parameterizedType.getActualTypeArguments());
                    types.put(clazz, parameterizedType);
                    loadTypes(clazz);
                }
            }
        }

        private void loadTypes(final Class<?> clazz) {
            if (clazz.getSuperclass() != null) {
                loadTypes(processAndResolveType(clazz.getGenericSuperclass(), clazz.getSuperclass()));
            }
            final Type[] genericInterfaces = clazz.getGenericInterfaces();
            final Class<?>[] interfaces = clazz.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                loadTypes(processAndResolveType(genericInterfaces[i], interfaces[i]));
            }
        }

        private Type processAndResolveType(final Type superclass, final Class<?> rawSuperclass) {
            if (superclass instanceof ParameterizedType) {
                final ParameterizedType parameterizedSuperclass = (ParameterizedType) superclass;
                processTypeVariables(rawSuperclass.getTypeParameters(), parameterizedSuperclass.getActualTypeArguments());
                return resolveType(parameterizedSuperclass);
            }
            if (superclass instanceof Class<?>) {
                return superclass;
            }
            throw new IllegalArgumentException("Superclass argument must be parameterized or a class, but got: " + superclass);
        }

        private void processTypeVariables(final TypeVariable<?>[] variables, final Type[] types) {
            for (int i = 0; i < variables.length; i++) {
                final Type type = types[i];
                final Type resolvedType = type instanceof TypeVariable<?> ? resolveType((TypeVariable<?>) type) : type;
                resolvedTypeVariables.put(variables[i], resolvedType);
            }
        }

        private Type resolveType(final TypeVariable<?> type) {
            return resolvedTypeVariables.getOrDefault(type, type);
        }

        private Type resolveType(final ParameterizedType type) {
            final Type[] unresolved = type.getActualTypeArguments();
            final Type[] resolved = new Type[unresolved.length];
            boolean modified = false; // potentially no need to re-create ParameterizedType
            for (int i = 0; i < unresolved.length; i++) {
                final Type unresolvedType = unresolved[i];
                Type resolvedType = unresolvedType;
                if (resolvedType instanceof TypeVariable<?>) {
                    resolvedType = resolveType((TypeVariable<?>) resolvedType);
                } // else if?
                if (resolvedType instanceof ParameterizedType) {
                    resolvedType = resolveType((ParameterizedType) resolvedType);
                }
                resolved[i] = resolvedType;
                if (resolvedType != unresolvedType) {
                    modified = true;
                }
            }
            if (!modified) {
                return type;
            }
            return new ParameterizedTypeImpl(type.getOwnerType(), type.getRawType(), resolved);
        }

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

    private static Type getReferenceType(final Type type) {
        return type instanceof Class<?> ? getReferenceType((Class<?>) type) : type;
    }

    public static <T> T cast(final Object o) {
        @SuppressWarnings("unchecked") final T t = (T) o;
        return t;
    }

    public static ParameterizedType getParameterizedType(final Type rawType, final Type... typeArguments) {
        return new ParameterizedTypeImpl(null, rawType, typeArguments);
    }

    public static Set<Class<?>> getImplementedInterfaces(final Class<?> type) {
        final Set<Class<?>> interfaces = new LinkedHashSet<>(List.of(type.getInterfaces()));
        for (Class<?> superclass = type.getSuperclass(); superclass != null; superclass = superclass.getSuperclass()) {
            interfaces.addAll(List.of(superclass.getInterfaces()));
        }
        return interfaces;
    }

}
