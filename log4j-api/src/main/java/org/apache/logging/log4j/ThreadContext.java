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

package org.apache.logging.log4j;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.spi.DefaultThreadContextMap;
import org.apache.logging.log4j.spi.DefaultThreadContextStack;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextStack;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ProviderUtil;

/**
 * The ThreadContext allows applications to store information either in a Map or a Stack.
 * <p>
 * <b><em>The MDC is managed on a per thread basis</em></b>. A child thread automatically inherits a <em>copy</em> of
 * the mapped diagnostic context of its parent.
 * </p>
 */
public final class ThreadContext  {

    /**
     * An empty read-only ThreadContextStack.
     */
    private static class EmptyThreadContextStack extends AbstractCollection<String> implements ThreadContextStack {

        private static final long serialVersionUID = 1L;

        private static final Iterator<String> EMPTY_ITERATOR = new EmptyIterator<String>();

        @Override
        public String pop() {
            return null;
        }

        @Override
        public String peek() {
            return null;
        }

        @Override
        public void push(final String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getDepth() {
            return 0;
        }

        @Override
        public List<String> asList() {
            return Collections.emptyList();
        }

        @Override
        public void trim(final int depth) {
            // Do nothing
        }

        @Override
        public boolean equals(final Object o) {
            // Similar to java.util.Collections.EmptyList.equals(Object)
            return (o instanceof Collection) && ((Collection<?>) o).isEmpty();
        }

        @Override
        public int hashCode() {
            // Same as java.util.Collections.EmptyList.hashCode()
            return 1;
        }
        
        @Override
        public ContextStack copy() {
            return this;
        }

        @Override
        public <T> T[] toArray(final T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(final String e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(final Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(final Collection<? extends String> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(final Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(final Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<String> iterator() {
            return EMPTY_ITERATOR;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public ContextStack getImmutableStackOrNull() {
            return this;
        }
    }

    /**
     * An empty iterator. Since Java 1.7 added the Collections.emptyIterator() method, we have to make do.
     * @param <E> the type of the empty iterator
     */
    private static class EmptyIterator<E> implements Iterator<E> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public E next() {
            throw new NoSuchElementException("This is an empty iterator!");
        }

        @Override
        public void remove() {
            // no-op
        }
    }

    /**
     * Empty, immutable Map.
     */
    // ironically, this annotation gives an "unsupported @SuppressWarnings" warning in Eclipse
    @SuppressWarnings("PublicStaticCollectionField") // I like irony, so I won't delete it...
    public static final Map<String, String> EMPTY_MAP = Collections.emptyMap();

    /**
     * Empty, immutable ContextStack.
     */
    // ironically, this annotation gives an "unsupported @SuppressWarnings" warning in Eclipse
    @SuppressWarnings("PublicStaticCollectionField")
    public static final ThreadContextStack EMPTY_STACK = new EmptyThreadContextStack();

    private static final String DISABLE_MAP = "disableThreadContextMap";
    private static final String DISABLE_STACK = "disableThreadContextStack";
    private static final String DISABLE_ALL = "disableThreadContext";
    private static final String THREAD_CONTEXT_KEY = "log4j2.threadContextMap";

    private static boolean disableAll;
    private static boolean useMap;
    private static boolean useStack;
    private static ThreadContextMap contextMap;
    private static ThreadContextStack contextStack;
    private static final Logger LOGGER = StatusLogger.getLogger();

    static {
        init();
    }
        
    /**
     * <em>Consider private, used for testing.</em>
     */
    static void init() {
        contextMap = null;
        final PropertiesUtil managerProps = PropertiesUtil.getProperties();
        disableAll = managerProps.getBooleanProperty(DISABLE_ALL);
        useStack = !(managerProps.getBooleanProperty(DISABLE_STACK) || disableAll);
        useMap = !(managerProps.getBooleanProperty(DISABLE_MAP) || disableAll);

        contextStack = new DefaultThreadContextStack(useStack);
        final String threadContextMapName = managerProps.getStringProperty(THREAD_CONTEXT_KEY);
        final ClassLoader cl = ProviderUtil.findClassLoader();
        if (threadContextMapName != null) {
            try {
                final Class<?> clazz = cl.loadClass(threadContextMapName);
                if (ThreadContextMap.class.isAssignableFrom(clazz)) {
                    contextMap = (ThreadContextMap) clazz.newInstance();
                }
            } catch (final ClassNotFoundException cnfe) {
                LOGGER.error("Unable to locate configured ThreadContextMap {}", threadContextMapName);
            } catch (final Exception ex) {
                LOGGER.error("Unable to create configured ThreadContextMap {}", threadContextMapName, ex);
            }
        }
        if (contextMap == null && ProviderUtil.hasProviders()) {
            final String factoryClassName = LogManager.getFactory().getClass().getName();
            for (final Provider provider : ProviderUtil.getProviders()) {
                if (factoryClassName.equals(provider.getClassName())) {
                    final Class<? extends ThreadContextMap> clazz = provider.loadThreadContextMap();
                    if (clazz != null) {
                        try {
                            contextMap = clazz.newInstance();
                            break;
                        } catch (final Exception e) {
                            LOGGER.error("Unable to locate or load configured ThreadContextMap {}",
                                provider.getThreadContextMap(), e);
                            contextMap = new DefaultThreadContextMap(useMap);
                        }
                    }
                }
            }
        }
        if (contextMap == null) {
            contextMap = new DefaultThreadContextMap(useMap);
        }
    }

    private ThreadContext() {
        // empty
    }

    /**
     * Puts a context value (the <code>value</code> parameter) as identified
     * with the <code>key</code> parameter into the current thread's
     * context map.
     *
     * <p>If the current thread does not have a context map it is
     * created as a side effect.</p>
     * @param key The key name.
     * @param value The key value.
     */
    public static void put(final String key, final String value) {
        contextMap.put(key, value);
    }

    /**
     * Gets the context value identified by the <code>key</code> parameter.
     *
     * <p>This method has no side effects.</p>
     * @param key The key to locate.
     * @return The value associated with the key or null.
     */
    public static String get(final String key) {
        return contextMap.get(key);
    }

    /**
     * Removes the context value identified by the <code>key</code> parameter.
     * @param key The key to remove.
     */
    public static void remove(final String key) {
        contextMap.remove(key);
    }

    /**
     * Clears the context map.
     */
    public static void clearMap() {
        contextMap.clear();
    }

    /**
     * Clears the context map and stack.
     */
    public static void clearAll() {
        clearMap();
        clearStack();
    }

    /**
     * Determines if the key is in the context.
     * @param key The key to locate.
     * @return True if the key is in the context, false otherwise.
     */
    public static boolean containsKey(final String key) {
        return contextMap.containsKey(key);
    }

    /**
     * Returns a mutable copy of current thread's context Map.
     * @return a mutable copy of the context.
     */
    public static Map<String, String> getContext() {
        return contextMap.getCopy();
    }

    /**
     * Returns an immutable view of the current thread's context Map.
     * @return An immutable view of the ThreadContext Map.
     */
    public static Map<String, String> getImmutableContext() {
        final Map<String, String> map = contextMap.getImmutableMapOrNull();
        return map == null ? EMPTY_MAP : map;
    }

    /**
     * Returns true if the Map is empty.
     * @return true if the Map is empty, false otherwise.
     */
    public static boolean isEmpty() {
        return contextMap.isEmpty();
    }

    /**
     * Clears the stack for this thread.
     */
    public static void clearStack() {
        contextStack.clear();
    }

    /**
     * Returns a copy of this thread's stack.
     * @return A copy of this thread's stack.
     */
    public static ContextStack cloneStack() {
        return contextStack.copy();
    }

    /**
     * Gets an immutable copy of this current thread's context stack.
     * @return an immutable copy of the ThreadContext stack.
     */
    public static ContextStack getImmutableStack() {
        final ContextStack result = contextStack.getImmutableStackOrNull();
        return result == null ? EMPTY_STACK : result;
    }

    /**
     * Sets this thread's stack.
     * @param stack The stack to use.
     */
    public static void setStack(final Collection<String> stack) {
        if (stack.isEmpty() || !useStack) {
            return;
        }
        contextStack.clear();
        contextStack.addAll(stack);
    }

    /**
     * Gets the current nesting depth of this thread's stack.
     * @return the number of items in the stack.
     *
     * @see #trim
     */
    public static int getDepth() {
        return contextStack.getDepth();
    }

    /**
     * Returns the value of the last item placed on the stack.
     *
     * <p>The returned value is the value that was pushed last. If no
     * context is available, then the empty string "" is returned.</p>
     *
     * @return String The innermost diagnostic context.
     */
    public static String pop() {
        return contextStack.pop();
    }

    /**
     * Looks at the last diagnostic context at the top of this NDC
     * without removing it.
     *
     * <p>The returned value is the value that was pushed last. If no
     * context is available, then the empty string "" is returned.</p>
     *
     * @return String The innermost diagnostic context.
     */
    public static String peek() {
        return contextStack.peek();
    }

    /**
     * Pushes new diagnostic context information for the current thread.
     *
     * <p>The contents of the <code>message</code> parameter is
     * determined solely by the client.</p>
     *
     * @param message The new diagnostic context information.
     */
    public static void push(final String message) {
        contextStack.push(message);
    }
    /**
     * Pushes new diagnostic context information for the current thread.
     *
     * <p>The contents of the <code>message</code> and args parameters are
     * determined solely by the client. The message will be treated as a format String
     * and tokens will be replaced with the String value of the arguments in accordance
     * with ParameterizedMessage.</p>
     *
     * @param message The new diagnostic context information.
     * @param args Parameters for the message.
     */
    public static void push(final String message, final Object... args) {
        contextStack.push(ParameterizedMessage.format(message, args));
    }

    /**
     * Removes the diagnostic context for this thread.
     *
     * <p>Each thread that created a diagnostic context by calling
     * {@link #push} should call this method before exiting. Otherwise,
     * the memory used by the <b>thread</b> cannot be reclaimed by the
     * VM.</p>
     *
     * <p>As this is such an important problem in heavy duty systems and
     * because it is difficult to always guarantee that the remove
     * method is called before exiting a thread, this method has been
     * augmented to lazily remove references to dead threads. In
     * practice, this means that you can be a little sloppy and
     * occasionally forget to call {@link #remove} before exiting a
     * thread. However, you must call <code>remove</code> sometime. If
     * you never call it, then your application is sure to run out of
     * memory.</p>
     */
    public static void removeStack() {
        contextStack.clear();
    }

    /**
     * Trims elements from this diagnostic context. If the current
     * depth is smaller or equal to <code>maxDepth</code>, then no
     * action is taken. If the current depth is larger than newDepth
     * then all elements at maxDepth or higher are discarded.
     *
     * <p>This method is a convenient alternative to multiple {@link
     * #pop} calls. Moreover, it is often the case that at the end of
     * complex call sequences, the depth of the ThreadContext is
     * unpredictable. The <code>trim</code> method circumvents
     * this problem.</p>
     *
     * <p>For example, the combination</p>
     * <pre>
     * void foo() {
     *     final int depth = ThreadContext.getDepth();
     *
     *     // ... complex sequence of calls
     *
     *     ThreadContext.trim(depth);
     * }
     * </pre>
     *
     * <p>ensures that between the entry and exit of {@code foo} the depth of the
     * diagnostic stack is conserved.</p>
     *
     * @see #getDepth
     * @param depth The number of elements to keep.
     */
    public static void trim(final int depth) {
        contextStack.trim(depth);
    }

    /**
     * The ThreadContext Stack interface.
     */
    public interface ContextStack extends Serializable, Collection<String> {

        /**
         * Returns the element at the top of the stack.
         * @return The element at the top of the stack.
         * @throws java.util.NoSuchElementException if the stack is empty.
         */
        String pop();

        /**
         * Returns the element at the top of the stack without removing it or null if the stack is empty.
         * @return the element at the top of the stack or null if the stack is empty.
         */
        String peek();

        /**
         * Pushes an element onto the stack.
         * @param message The element to add.
         */
        void push(String message);

        /**
         * Returns the number of elements in the stack.
         * @return the number of elements in the stack.
         */
        int getDepth();

        /**
         * Returns all the elements in the stack in a List.
         * @return all the elements in the stack in a List.
         */
        List<String> asList();

        /**
         * Trims elements from the end of the stack.
         * @param depth The maximum number of items in the stack to keep.
         */
        void trim(int depth);

        /**
         * Returns a copy of the ContextStack.
         * @return a copy of the ContextStack.
         */
        ContextStack copy();

        /**
         * Returns a ContextStack with the same contents as this ContextStack or {@code null}.
         * Attempts to modify the returned stack may or may not throw an exception, but will not affect the contents
         * of this ContextStack.
         * @return a ContextStack with the same contents as this ContextStack or {@code null}.
         */
        ContextStack getImmutableStackOrNull();
    }
}
