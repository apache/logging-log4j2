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
import java.util.function.Supplier;

import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.Strings;

/**
 * Type with an optional {@link QualifierType} type, name, and namespace. Keys are used for binding to and looking up instance
 * factories via {@link Injector}.
 *
 * @param <T> type of key
 */
public class Key<T> {
    private final Type type;
    private final Class<T> rawType;
    private final Class<? extends Annotation> qualifierType;
    private final String name;
    private final String namespace;
    private final int order;
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
        rawType = Cast.cast(TypeUtil.getRawType(type));
        final AnnotatedType superclass = getClass().getAnnotatedSuperclass();
        final Annotation qualifier = AnnotationUtil.getMetaAnnotation(superclass, QualifierType.class);
        qualifierType = qualifier != null ? qualifier.annotationType() : null;
        name = Keys.getName(superclass);
        namespace = Keys.getNamespace(superclass);
        order = getOrder(superclass);
        hashCode = Objects.hash(type, qualifierType, name.toLowerCase(Locale.ROOT), namespace.toLowerCase(Locale.ROOT));
    }

    private Key(
            final Type type, final Class<T> rawType, final Class<? extends Annotation> qualifierType, final String name,
            final String namespace, final int order) {
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
     */
    public final Class<T> getRawType() {
        return rawType;
    }

    /**
     * Returns the name of this key. Names are case-insensitive. If this key has no defined name, then this returns
     * an empty string.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the namespace of this key. If this key has no defined namespace, then this returns an empty string.
     */
    public final String getNamespace() {
        return namespace;
    }

    /**
     * Returns the ordinal value of this key. Keys that are otherwise equal can be compared by this
     * ordinal using the natural integer comparator where ties should default to keeping an existing binding intact.
     */
    public final int getOrder() {
        return order;
    }

    /**
     * Returns a new key using the provided name and the same type and qualifier type as this instance.
     */
    public final Key<T> withName(final String name) {
        return builder(this).setName(name).get();
    }

    /**
     * Returns a new key using the provided namespace otherwise populated with the same values as this instance.
     */
    public final Key<T> withNamespace(final String namespace) {
        return builder(this).setNamespace(namespace).get();
    }

    /**
     * Returns a new key using the provided qualifier type and the same type and name as this instance.
     */
    public final Key<T> withQualifierType(final Class<? extends Annotation> qualifierType) {
        return builder(this).setQualifierType(qualifierType).get();
    }

    /**
     * If this key's type {@code T} is a subtype of {@code Supplier<P>} for some supplied type {@code P}, then this
     * returns a new key with that type argument along with the same name and qualifier type as this key.
     */
    public final <P> Key<P> getSuppliedType() {
        if (type instanceof ParameterizedType && Supplier.class.isAssignableFrom(rawType)) {
            final Type typeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
            return builder(this).<P>setType(typeArgument).get();
        }
        return null;
    }

    /**
     * If this key's type {@code T} is a parameterized type, then this returns a new key corresponding to the
     * requested parameterized type argument along with the same remaining values as this key.
     */
    public final <P> Key<P> getParameterizedTypeArgument(final int arg) {
        if (type instanceof ParameterizedType) {
            final Type typeArgument = ((ParameterizedType) type).getActualTypeArguments()[arg];
            return builder(this).<P>setType(typeArgument).get();
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
            toString = string = String.format("Key{namespace='%s', name='%s', type=%s, qualifierType=%s}",
                    namespace, name, type.getTypeName(), qualifierType != null ? qualifierType.getSimpleName() : Strings.EMPTY);
        }
        return string;
    }

    /**
     * Creates a Key for the class.
     */
    public static <T> Key<T> forClass(final Class<T> clazz) {
        return builder()
                .setType(clazz)
                .setQualifierType(getQualifierType(clazz))
                .setName(Keys.getName(clazz))
                .setNamespace(Keys.getNamespace(clazz))
                .setOrder(getOrder(clazz))
                .get();
    }

    /**
     * Creates a Key for the return type of the method.
     */
    public static <T> Key<T> forMethod(final Method method) {
        return builder()
                .<T>setType(method.getGenericReturnType())
                .setQualifierType(getQualifierType(method))
                .setName(Keys.getName(method))
                .setNamespace(Keys.getNamespace(method))
                .setOrder(getOrder(method))
                .get();
    }

    /**
     * Creates a Key for the parameter.
     */
    public static <T> Key<T> forParameter(final Parameter parameter) {
        return builder()
                .<T>setType(parameter.getParameterizedType())
                .setQualifierType(getQualifierType(parameter))
                .setName(Keys.getName(parameter))
                .setNamespace(Keys.getNamespace(parameter))
                .get();
    }

    /**
     * Creates a Key for the field.
     */
    public static <T> Key<T> forField(final Field field) {
        return builder()
                .<T>setType(field.getGenericType())
                .setQualifierType(getQualifierType(field))
                .setName(Keys.getName(field))
                .setNamespace(Keys.getNamespace(field))
                .get();
    }

    /**
     * Creates a new key builder.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Creates a new key builder from an existing key's properties.
     */
    public static <T> Builder<T> builder(final Key<T> original) {
        return new Builder<>(original);
    }

    private static Class<? extends Annotation> getQualifierType(final AnnotatedElement element) {
        final Annotation qualifierAnnotation = AnnotationUtil.getMetaAnnotation(element, QualifierType.class);
        return qualifierAnnotation != null ? qualifierAnnotation.annotationType() : null;
    }

    private static int getOrder(final AnnotatedElement element) {
        final Ordered annotation = element.getAnnotation(Ordered.class);
        return annotation != null ? annotation.value() : 0;
    }

    /**
     * Builder class for configuring a new {@link Key} instance.
     *
     * @param <T> type of key
     */
    public static class Builder<T> implements Supplier<Key<T>> {
        private Type type;
        private Class<? extends Annotation> qualifierType;
        private String name;
        private String namespace;
        private Integer order;

        private Builder() {
        }

        private Builder(final Key<T> original) {
            type = original.type;
            qualifierType = original.qualifierType;
            name = original.name;
            namespace = original.namespace;
            order = original.order;
        }

        /**
         * Specifies the generic type of the key.
         */
        public <U> Builder<U> setType(final Type type) {
            this.type = type;
            return Cast.cast(this);
        }

        /**
         * Specifies the type of key using a class reference.
         */
        public <U> Builder<U> setType(final Class<U> type) {
            this.type = type;
            return Cast.cast(this);
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
            this.order = order;
            return this;
        }

        /**
         * Creates a new {@link Key} from this builder's properties.
         */
        @Override
        public Key<T> get() {
            if (name == null) {
                name = Strings.EMPTY;
            }
            if (namespace == null) {
                namespace = Strings.EMPTY;
            }
            final Class<T> rawType = Cast.cast(TypeUtil.getRawType(type));
            int order = this.order != null ? this.order : getOrder(rawType);
            return new Key<>(type, rawType, qualifierType, name, namespace, order);
        }
    }
}
