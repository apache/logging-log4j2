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

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.FactoryType;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.convert.TypeConverter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Supplier;

/**
 * Manages dependency injection of a set of bindings between {@link Key}s and {@link Supplier}s lifecycle-bound to
 * {@link Scope}s. Keys describe the type, name, namespace, qualifier type, and order of a binding. Suppliers are known
 * as <i>factories</i>, and factories may have injectable dependencies on other bindings upon creation. Scopes control
 * the lifecycle of instances returned by a binding's factory.
 *
 * <p>When first creating an Injector, invocation of {@link #init()} will invoke all registered {@link InjectorCallback}
 * services in {@linkplain InjectorCallback#getOrder() numeric order}. These callbacks may register bindings
 * {@linkplain #registerBinding(Key, Supplier) programmatically} or via {@linkplain #registerBundle(Object) bundle classes},
 * and set a {@linkplain #setReflectionAccessor(ReflectionAccessor) default reflection accessor} for customizing the
 * context in which private reflection operations are performed. Additional scopes
 * {@linkplain #registerScope(Class, Scope) may also be registered}.</p>
 *
 * <p>Factories for keys can be looked up {@linkplain #getFactory(Key) by key} or {@linkplain #getFactory(Class) by class}.
 * Any object created outside this system can have {@linkplain #injectMembers(Object) its members injected}.</p>
 *
 * <p>A parsed {@linkplain Node configuration node} can be {@linkplain #configure(Node) configured} using its referenced
 * plugin class to return the plugin instance. Configuring a node configures its children nodes and consumes its
 * attributes before returning the plugin instance.</p>
 */
public interface Injector {
    /**
     * The key corresponding to the current Injector.
     */
    Key<Injector> KEY = new Key<>() {};

    /**
     * Initializes this Injector with all registered {@link InjectorCallback} services in
     * {@linkplain InjectorCallback#getOrder() integral order}. This method should only be invoked after construction
     * of a fresh Injector.
     */
    void init();

    /**
     * Creates a new Injector copied from the current bindings and scopes of this instance. Subsequent changes to this
     * Injector or the returned copy are independent of each other.
     *
     * @return a fresh copy of this instance
     */
    Injector copy();

    /**
     * Gets a factory for instances that match the given key.
     *
     * @param key key to look up a factory for
     * @param <T> return type of factory
     * @return a factory for the provided key
     */
    <T> Supplier<T> getFactory(final Key<T> key);

    /**
     * Gets a factory for instances of the provided class.
     *
     * @param clazz class to look up a factory for
     * @param <T>   return type of factory
     * @return a factory for the provided class
     */
    default <T> Supplier<T> getFactory(final Class<T> clazz) {
        return getFactory(Key.forClass(clazz));
    }

    /**
     * Gets an instance that matches the provided key.
     *
     * @param key key to get or create an instance matching
     * @param <T> type of instance
     * @return an instance matching the provided key
     */
    default <T> T getInstance(final Key<T> key) {
        return getFactory(key).get();
    }

    /**
     * Gets an instance for the provided class.
     *
     * @param clazz class to get or create an instance of
     * @param <T>   type of instance
     * @return an instance of the provided class
     */
    default <T> T getInstance(final Class<T> clazz) {
        return getFactory(clazz).get();
    }

    /**
     * Gets a TypeConverter for the provided type.
     *
     * @param type type to get a converter for
     * @return a TypeConverter for the provided type
     * @throws java.util.UnknownFormatConversionException if no converter can be found for the provided type
     */
    TypeConverter<?> getTypeConverter(final Type type);

    /**
     * Injects dependencies into the members of the provided instance. Injectable fields are set, then injectable methods are
     * invoked (first those with parameters, then those without parameters).
     *
     * @param instance instance in which to inject member dependencies
     */
    void injectMembers(final Object instance);

    /**
     * Creates a plugin instance using the provided configuration node. Unless the plugin
     * {@linkplain Configurable#deferChildren() defers children nodes}, child nodes are configured first before
     * configuring this node. Each node's plugin should consume all supported
     * {@linkplain org.apache.logging.log4j.plugins.PluginAttribute attributes} through dependency injection along with
     * any other instances that can be provided through
     * {@linkplain org.apache.logging.log4j.plugins.visit.NodeVisitor node visitor strategies} or general bindings from
     * {@link #getFactory(Key)}. The end result of {@link Node#getObject()} is returned for convenience.
     *
     * @param node configuration node containing a plugin type, attributes, and child nodes to consume for dependency injection
     * @param <T>  type of instance the given node configures
     * @return the configured plugin instance
     */
    <T> T configure(final Node node);

    /**
     * Registers a scope annotation type to the given Scope strategy.
     *
     * @param scopeType scope annotation type
     * @param scope     scope strategy to use for the given scope type
     */
    void registerScope(final Class<? extends Annotation> scopeType, final Scope scope);

    /**
     * Gets the registered Scope strategy for the given scope annotation type.
     *
     * @param scopeType scope annotation type
     * @return the registered scope instance for the provided scope type
     */
    Scope getScope(final Class<? extends Annotation> scopeType);

    /**
     * Registers a bundle into this Injector. A bundle is an instance of a class with methods annotated with
     * {@link FactoryType}-annotated annotations which provide dependency-injected bindings. Bindings registered
     * via bundles are merged with the existing bindings in this Injector based on the {@linkplain Key#getOrder() order}
     * of the methods to determine ambiguities.
     *
     * @param bundle bundle to install with factory methods for factories
     */
    void registerBundle(final Object bundle);

    /**
     * Registers a binding between a key and factory. This overwrites any existing binding the key may have had.
     *
     * @param key     key of the factory to create a binding with
     * @param factory the factory to bind to the key
     * @param <T>     the return type of the factory
     * @return this Injector (useful for chaining)
     */
    <T> Injector registerBinding(final Key<T> key, final Supplier<? extends T> factory);

    /**
     * Binds a key to a factory only if no bindings for that key already exist. This is useful for registering default
     * bindings or bridging legacy methods of configuring bindings.
     *
     * @param key     key of the factory to create a binding with if none exist
     * @param factory the factory to bind to the key
     * @param <T>     the return type of the factory
     * @return this Injector (useful for chaining)
     */
    <T> Injector registerBindingIfAbsent(final Key<T> key, final Supplier<? extends T> factory);

    /**
     * Removes any existing binding for the provided key.
     *
     * @param key key to remove previously registered bindings
     */
    void removeBinding(final Key<?> key);

    /**
     * Indicates if a binding exists for the provided key.
     */
    boolean hasBinding(final Key<?> key);

    /**
     * Sets the {@link ReflectionAccessor} used for invoking {@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}
     * from an appropriate caller class. Customizing this allows for changing the base module that other modules should
     * open themselves to.
     *
     * @param accessor accessor to use
     */
    void setReflectionAccessor(final ReflectionAccessor accessor);
}
