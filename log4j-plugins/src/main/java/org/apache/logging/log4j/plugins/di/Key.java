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
package org.apache.logging.log4j.plugins.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.plugins.di.spi.InjectionPoint;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;
import org.apache.logging.log4j.plugins.di.spi.Scope;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.Strings;

/**
 * Keys represent a reified type with an optional {@link QualifierType} type, name, and namespace.
 * A key is used in two related contexts: describing an {@link InjectionPoint} requesting a dependency
 * for injection and describing a {@link Binding} where a {@linkplain Scope scoped} factory is
 * registered for providing dependencies.
 *
 * @param <T> type of key
 * @see InstanceFactory
 * @see ConfigurableInstanceFactory
 * @see Keys
 * @see ResolvableKey
 */
public class Key<T> implements StringBuilderFormattable, Comparable<Key<T>> {
    private final Type type;
    private final Class<T> rawType;
    private final Class<? extends Annotation> qualifierType;
    private final String name;
    private final String namespace;
    private final OptionalInt order;
    private final int hashCode;
    private String toString;

    /**
     * Anonymous subclasses override this constructor to instantiate this Key based on the type given.
     * For example, to represent the equivalent of an annotated field {@code @Named("abc") Map<String, List<String>> field},
     * this constructor would be used like so:
     *
     * <pre>{@code
     * Key<Map<String, List<String>>> key = new @Named("abc") Key<>() {};
     * // or equivalently
     * var key = new @Named("abc") Key<Map<String, List<String>>>() {};
     * }</pre>
     */
    protected Key() {
        type = TypeUtil.getSuperclassTypeParameter(getClass());
        rawType = TypeUtil.getRawType(type);
        final AnnotatedType superclass = getClass().getAnnotatedSuperclass();
        final Annotation qualifier = AnnotationUtil.getElementAnnotationHavingMetaAnnotation(superclass, QualifierType.class);
        qualifierType = qualifier != null ? qualifier.annotationType() : null;
        name = Keys.getName(superclass);
        namespace = Keys.getNamespace(superclass);
        order = AnnotationUtil.getOrder(superclass);
        hashCode = Objects.hash(type, qualifierType, name.toLowerCase(Locale.ROOT), namespace.toLowerCase(Locale.ROOT));
    }

    private Key(
            final Type type, final Class<T> rawType, final Class<? extends Annotation> qualifierType, final String name,
            final String namespace, final OptionalInt order) {
        this.type = type;
        this.rawType = rawType;
        this.qualifierType = qualifierType;
        this.name = name;
        this.namespace = namespace;
        this.order = order;
        hashCode = Objects.hash(type, qualifierType, name.toLowerCase(Locale.ROOT), namespace.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns the generic type of this key.
     */
    public final Type getType() {
        return type;
    }

    /**
     * Returns the raw type of this key corresponding to its generic type.
     *
     * @see TypeUtil#getRawType(Type)
     */
    public final Class<T> getRawType() {
        return rawType;
    }

    /**
     * Returns the name of this key. If this key has no defined name, then this returns an empty string.
     *
     * @apiNote Names are case-preserving but should be compared using case-insensitive string comparison.
     * @see Keys#hasName(AnnotatedElement)
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the namespace of this key. If this key has no defined namespace, then this returns an empty string.
     *
     * @apiNote A namespace of the empty string is considered the default namespace which is used when no
     * namespace is defined or if the namespace is explicitly defined as the empty string.
     * @see Keys#getNamespace(AnnotatedElement)
     */
    public final String getNamespace() {
        return namespace;
    }

    /**
     * Returns the qualifier type of this key. If this key has no qualifier type defined, then this returns
     * {@code null}.
     */
    public final Class<? extends Annotation> getQualifierType() {
        return qualifierType;
    }

    /**
     * Returns the ordinal value of this key. Keys that are otherwise equal can be compared by this
     * ordinal using the natural integer comparator where ties should default to keeping an existing binding intact.
     */
    public final OptionalInt getOrder() {
        return order;
    }

    /**
     * Returns a new key using the provided type. The name, namespace, qualifier, and order of this key are
     * copied into the new key.
     */
    public final <U> Key<U> withType(final Type type) {
        final Class<U> rawType = TypeUtil.getRawType(type);
        return new Key<>(type, rawType, qualifierType, name, namespace, order);
    }

    /**
     * Returns a new key using the provided class. The name, namespace, qualifier, and order of this key are
     * copied into the new key.
     */
    public final <U> Key<U> withType(final Class<U> type) {
        return new Key<>(type, type, qualifierType, name, namespace, order);
    }

    /**
     * Returns a new key using the provided name and the same type and qualifier type as this instance.
     */
    public final Key<T> withName(final String name) {
        return new Key<>(type, rawType, qualifierType, name, namespace, order);
    }

    /**
     * Returns a new key using the provided namespace otherwise populated with the same values as this instance.
     */
    public final Key<T> withNamespace(final String namespace) {
        return new Key<>(type, rawType, qualifierType, name, namespace, order);
    }

    /**
     * Returns a new key using the provided qualifier type and the same type and name as this instance.
     */
    public final Key<T> withQualifierType(final Class<? extends Annotation> qualifierType) {
        return new Key<>(type, rawType, qualifierType, name, namespace, order);
    }

    /**
     * If this key's type {@code T} is a subtype of {@code Supplier<P>} for some supplied type {@code P}, then this
     * returns a new key with that type argument along with the same name and qualifier type as this key.
     *
     * @return a new instance from this using the supplied type or {@code null} if this key did not contain a
     * supplier type
     */
    public final <P> Key<P> getSuppliedType() {
        if (type instanceof ParameterizedType && Supplier.class.isAssignableFrom(rawType)) {
            final Type typeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
            return withType(typeArgument);
        }
        return null;
    }

    /**
     * If this key's type {@code T} is a parameterized type such that {@code T} can be represented as
     * {@code R<P0, P1, P2, ...>} with raw type {@code R} and type arguments {@code P0}, {@code P1}, etc.,
     * then this returns a new key corresponding to the requested parameterized type argument along with
     * the same remaining values as this key.
     *
     * @param arg the index of the type argument to create a new key from
     * @return a new instance from this using the type argument if defined or {@code null}
     * @throws IndexOutOfBoundsException if {@code arg} is negative or otherwise outside the bounds of the actual
     *                                   number of type arguments of this key's type
     */
    public final <P> Key<P> getParameterizedTypeArgument(final int arg) {
        if (arg < 0) {
            throw new IndexOutOfBoundsException(arg);
        }
        if (type instanceof ParameterizedType) {
            final Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
            if (arg > typeArguments.length) {
                throw new IndexOutOfBoundsException(arg);
            }
            final Type typeArgument = typeArguments[arg];
            return withType(typeArgument);
        }
        return null;
    }

    @Override
    public final boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Key<?>)) {
            return false;
        }
        final Key<?> that = (Key<?>) o;
        return TypeUtil.isEqual(this.type, that.type) &&
                this.name.equalsIgnoreCase(that.name) &&
                this.namespace.equalsIgnoreCase(that.namespace) &&
                Objects.equals(this.qualifierType, that.qualifierType);
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }

    @Override
    public final String toString() {
        String string = toString;
        if (string == null) {
            StringBuilder sb = new StringBuilder(32);
            formatTo(sb);
            toString = string = sb.toString();
        }
        return string;
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        buffer.append(TO_STRING_PREFIX).append(type.getTypeName());
        if (!namespace.isEmpty()) {
            buffer.append(NAMESPACE).append(namespace);
        }
        if (!name.isEmpty()) {
            buffer.append(NAME).append(name);
        }
        if (qualifierType != null) {
            buffer.append(QUALIFIER_TYPE).append(qualifierType.getSimpleName());
        }
        buffer.append(']');
    }

    @Override
    public int compareTo(final Key<T> o) {
        final OptionalInt otherOrder = o.order;
        if (order.isPresent() && otherOrder.isPresent()) {
            return Integer.compare(order.getAsInt(), otherOrder.getAsInt());
        }
        if (order.isPresent()) {
            return -1;
        }
        if (otherOrder.isPresent()) {
            return 1;
        }
        if (equals(o)) {
            return 0;
        }
        // FIXME: reconsider the use here or provide an alternative that throws an exception at this point
        return name.compareToIgnoreCase(o.name);
    }

    private static final String TO_STRING_PREFIX = "Key[type: ";
    private static final String NAMESPACE = "; namespace: ";
    private static final String NAME = "; name: ";
    private static final String QUALIFIER_TYPE = "; qualifierType: ";

    /**
     * Creates a Key for the class.
     */
    public static <T> Key<T> forClass(final Class<T> clazz) {
        final Builder<T> builder = Key.builder(clazz)
                .setQualifierType(getQualifierType(clazz))
                .setName(Keys.getName(clazz))
                .setNamespace(Keys.getNamespace(clazz));
        AnnotationUtil.getOrder(clazz).ifPresent(builder::setOrder);
        return builder.get();
    }

    /**
     * Creates a Key for the return type of the method.
     */
    public static <T> Key<T> forMethod(final Method method) {
        final Builder<T> builder = Key.builder(method.getGenericReturnType());
        AnnotationUtil.getOrder(method).ifPresent(builder::setOrder);
        return builder
                .setQualifierType(getQualifierType(method))
                .setName(Keys.getName(method))
                .setNamespace(Keys.getNamespace(method))
                .get();
    }

    /**
     * Creates a Key for the parameter.
     */
    public static <T> Key<T> forParameter(final Parameter parameter) {
        return Key.<T>builder(parameter.getParameterizedType())
                .setQualifierType(getQualifierType(parameter))
                .setName(Keys.getName(parameter))
                .setNamespace(Keys.getNamespace(parameter))
                .get();
    }

    /**
     * Creates a Key for the field.
     */
    public static <T> Key<T> forField(final Field field) {
        return Key.<T>builder(field.getGenericType())
                .setQualifierType(getQualifierType(field))
                .setName(Keys.getName(field))
                .setNamespace(Keys.getNamespace(field))
                .get();
    }

    /**
     * Creates a new key builder for the given generic type.
     */
    public static <T> Builder<T> builder(final Type type) {
        return new Builder<>(type);
    }

    /**
     * Creates a new key builder for the given class.
     */
    public static <T> Builder<T> builder(final Class<T> type) {
        return new Builder<>(type);
    }

    /**
     * Creates a new key builder from an existing key's properties.
     */
    public static <T> Builder<T> builder(final Key<T> original) {
        return new Builder<>(original);
    }

    private static Class<? extends Annotation> getQualifierType(final AnnotatedElement element) {
        return AnnotationUtil.findAnnotatedAnnotations(element, QualifierType.class)
                .map(annotatedAnnotation -> annotatedAnnotation.getAnnotation().annotationType())
                .findFirst()
                .orElse(null);
    }

    /**
     * Builder class for configuring a new {@link Key} instance.
     *
     * @param <T> type of key
     */
    public static class Builder<T> implements Supplier<Key<T>> {
        private final Type type;
        private final Class<T> rawType;
        private Class<? extends Annotation> qualifierType;
        private String name;
        private String namespace;
        private OptionalInt order = OptionalInt.empty();

        private Builder(final Type type) {
            this.type = type;
            rawType = TypeUtil.getRawType(type);
        }

        private Builder(final Class<T> type) {
            this.type = type;
            rawType = type;
        }

        private Builder(final Key<T> original) {
            type = original.type;
            rawType = original.rawType;
            qualifierType = original.qualifierType;
            name = original.name;
            namespace = original.namespace;
            order = original.order;
        }

        /**
         * Specifies a qualifier annotation type. Qualifiers are optional and are used for an additional comparison
         * property for keys.
         */
        public Builder<T> setQualifierType(final Class<? extends Annotation> qualifierType) {
            this.qualifierType = qualifierType;
            return this;
        }

        /**
         * Specifies the name of this key. The default name for keys is the empty string.
         */
        public Builder<T> setName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Specifies the namespace of this key. The default namespace for keys is the empty string.
         */
        public Builder<T> setNamespace(final String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Specifies the order of this key for disambiguation. This overrides any value discovered from
         * the {@link Ordered} annotation on the type of the key.
         */
        public Builder<T> setOrder(final int order) {
            this.order = OptionalInt.of(order);
            return this;
        }

        /**
         * Creates a new {@link Key} from this builder's properties.
         */
        @Override
        public Key<T> get() {
            if (Strings.isBlank(name)) {
                name = Strings.EMPTY;
            }
            if (Strings.isBlank(namespace)) {
                namespace = Strings.EMPTY;
            }
            OptionalInt order = this.order;
            if (order.isEmpty()) {
                order = AnnotationUtil.getOrder(rawType);
            }
            return new Key<>(type, rawType, qualifierType, name, namespace, order);
        }
    }
}
