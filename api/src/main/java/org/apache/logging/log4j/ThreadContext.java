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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * The ThreadContext allows applications to store information either in a Map
 * <p/>
 * <p><b><em>The MDC is managed on a per thread basis</em></b>. A
 * child thread automatically inherits a <em>copy</em> of the mapped
 * diagnostic context of its parent.
 */
public final class ThreadContext {

    /**
     * Empty, immutable Map.
     */
    public static final Map<String, String> EMPTY_MAP = new ImmutableMap();

    /**
     * Empty, immutable ContextStack.
     */
    public static final ContextStack EMPTY_STACK = new ImmutableStack();

    private static final String DISABLE_MAP = "disableThreadContextMap";

    private static final String DISABLE_STACK = "disableThreadContextStack";

    private static final String DISABLE_ALL = "disableThreadContext";

    private static boolean all = Boolean.getBoolean(DISABLE_ALL);

    private static boolean useMap = !(Boolean.getBoolean(DISABLE_MAP) || all);

    private static boolean useStack = !(Boolean.getBoolean(DISABLE_STACK) || all);

    private static ThreadLocal<Map<String, String>> localMap =
        new InheritableThreadLocal<Map<String, String>>() {
            @Override
            protected Map<String, String> childValue(Map<String, String> parentValue) {
                return parentValue == null || !useMap ? null : new HashMap<String, String>(parentValue);
            }
        };

    private static ThreadLocal<ContextStack> localStack = new ThreadLocal<ContextStack>();

    private ThreadContext() {

    }

    /**
     * Put a context value (the <code>o</code> parameter) as identified
     * with the <code>key</code> parameter into the current thread's
     * context map.
     * <p/>
     * <p>If the current thread does not have a context map it is
     * created as a side effect.
     * @param key The key name.
     * @param value The key value.
     */
    public static void put(String key, String value) {
        if (!useMap) {
            return;
        }
        Map<String, String> map = localMap.get();
        if (map == null) {
            map = new HashMap<String, String>();
            localMap.set(map);
        }
        map.put(key, value);
    }

    /**
     * Get the context identified by the <code>key</code> parameter.
     * <p/>
     * <p>This method has no side effects.
     * @param key The key to locate.
     * @return The value associated with the key or null.
     */
    public static String get(String key) {
        Map<String, String> map = localMap.get();
        return map == null ? null : map.get(key);
    }

    /**
     * Remove the the context identified by the <code>key</code>
     * parameter.
     * @param key The key to remove.
     */
    public static void remove(String key) {
        Map<String, String> map = localMap.get();
        if (map != null) {
            map.remove(key);
        }
    }

    /**
     * Clear the context.
     */
    public static void clear() {
        localMap.remove();
    }

    /**
     * Determine if the key is in the context.
     * @param key The key to locate.
     * @return True if the key is in the context, false otherwise.
     */
    public static boolean containsKey(String key) {
        Map<String, String> map = localMap.get();
        return map == null ? false : map.containsKey(key);
    }

    /**
     * Get a copy of current thread's context Map.
     * @return a copy of the context.
     */
    public static Map<String, String> getContext() {
        Map<String, String> map = localMap.get();
        return map == null ? new HashMap<String, String>() : new HashMap<String, String>(map);
    }

    /**
     * Get an immutable copy of the current thread's context Map.
     * @return An immutable copy of the ThreadContext Map.
     */
    public static Map<String, String> getImmutableContext() {
        Map<String, String> map = localMap.get();
        return map == null ? new ImmutableMap() : new ImmutableMap(map);
    }

    /**
     * Returns true if the Map is empty.
     * @return true if the Map is empty, false otherwise.
     */
    public static boolean isEmpty() {
        Map<String, String> map = localMap.get();
        return map == null || map.size() == 0;
    }

    /**
     * Clear the stack for this thread.
     */
    public static void clearStack() {
        localStack.remove();
    }

    /**
     * Returns a copy of this thread's stack.
     * @return A copy of this thread's stack.
     */
    public static ContextStack cloneStack() {
        ContextStack stack = localStack.get();
        return stack == null ? new ThreadContextStack() : new ThreadContextStack(stack.asList());
    }

    /**
     * Get an immutable copy of this current thread's context stack.
     * @return an immutable copy of the ThreadContext stack.
     */
    public static ContextStack getImmutableStack() {
        ContextStack stack = localStack.get();
        return stack == null ? EMPTY_STACK : new ImmutableStack(stack.asList());
    }

    /**
     * Set this thread's stack.
     * @param stack The stack to use.
     */
    public static void setStack(Collection<String> stack) {
        if (stack.size() == 0 || !useStack) {
            return;
        }
        localStack.set(new ThreadContextStack(stack));
    }

    /**
     * Get the current nesting depth of this thread's stack.
     * @return the number of items in the stack.
     *
     * @see #trim
     */
    public static int getDepth() {
        ContextStack stack = localStack.get();
        return stack == null ? 0 : stack.getDepth();
    }

    /**
     * Returns the value of the last item placed on the stack.
     * <p/>
     * <p>The returned value is the value that was pushed last. If no
     * context is available, then the empty string "" is returned.
     *
     * @return String The innermost diagnostic context.
     */
    public static String pop() {
        ContextStack s = localStack.get();
        if (s == null || s.getDepth() == 0) {
            return "";
        }
        return s.pop();
    }

    /**
     * Looks at the last diagnostic context at the top of this NDC
     * without removing it.
     * <p/>
     * <p>The returned value is the value that was pushed last. If no
     * context is available, then the empty string "" is returned.
     *
     * @return String The innermost diagnostic context.
     */
    public static String peek() {
        ContextStack s = localStack.get();
        if (s == null || s.getDepth() == 0) {
            return "";
        }
        return s.peek();
    }

    /**
     * Push new diagnostic context information for the current thread.
     * <p/>
     * <p>The contents of the <code>message</code> parameter is
     * determined solely by the client.
     *
     * @param message The new diagnostic context information.
     */
    public static void push(String message) {
        if (!useStack) {
            return;
        }
        ContextStack stack = localStack.get();
        if (stack == null) {
            stack = new ThreadContextStack();
            localStack.set(stack);
        }
        stack.push(message);
    }

    /**
     * Remove the diagnostic context for this thread.
     * <p/>
     * <p>Each thread that created a diagnostic context by calling
     * {@link #push} should call this method before exiting. Otherwise,
     * the memory used by the <b>thread</b> cannot be reclaimed by the
     * VM.
     * <p/>
     * <p>As this is such an important problem in heavy duty systems and
     * because it is difficult to always guarantee that the remove
     * method is called before exiting a thread, this method has been
     * augmented to lazily remove references to dead threads. In
     * practice, this means that you can be a little sloppy and
     * occasionally forget to call {@link #remove} before exiting a
     * thread. However, you must call <code>remove</code> sometime. If
     * you never call it, then your application is sure to run out of
     * memory.
     */
    public static void removeStack() {
        localStack.remove();
    }

    /**
     * Trims elements from this diagnostic context. If the current
     * depth is smaller or equal to <code>maxDepth</code>, then no
     * action is taken. If the current depth is larger than newDepth
     * then all elements at maxDepth or higher are discarded.
     * <p/>
     * <p>This method is a convenient alternative to multiple {@link
     * #pop} calls. Moreover, it is often the case that at the end of
     * complex call sequences, the depth of the ThreadContext is
     * unpredictable. The <code>trim</code> method circumvents
     * this problem.
     * <p/>
     * <p>For example, the combination
     * <pre>
     * void foo() {
     * &nbsp;  int depth = ThreadContext.getDepth();
     * <p/>
     * &nbsp;  ... complex sequence of calls
     * <p/>
     * &nbsp;  ThreadContext.trim(depth);
     * }
     * </pre>
     * <p/>
     * ensures that between the entry and exit of foo the depth of the
     * diagnostic stack is conserved.
     *
     * @see #getDepth
     * @param depth The number of elements to keep.
     */
    public static void trim(int depth) {
        ContextStack stack = localStack.get();
        if (stack != null) {
            stack.trim(depth);
        }
    }

    /**
     * The ThreadContext Stack interface.
     */
    public interface ContextStack extends Serializable {

        /**
         * Clears all elements from the stack.
         */
        void clear();

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
         * Add an element to the stack.
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
    }

    /**
     * The ContextStack implementation.
     */
    private static class ThreadContextStack extends ArrayList<String> implements ContextStack {

        private static final long serialVersionUID = 5050501L;

        public ThreadContextStack() {
            super();
        }

        public ThreadContextStack(Collection<String> collection) {
            super(collection);
        }

        public String pop() {
            int index = size() - 1;
            if (index >= 0) {
                String result = get(index);
                remove(index);
                return result;
            }
            throw new NoSuchElementException("The ThreadContext stack is empty");
        }

        public String peek() {
            int index = size() - 1;
            if (index >= 0) {
                return get(index);
            }
            return null;
        }

        public void push(String message) {
            add(message);
        }

        public int getDepth() {
            return size();
        }

        public List<String> asList() {
            return this;
        }

        public void trim(int depth) {
            if (depth < 0) {
                throw new IllegalArgumentException("Maximum stack depth cannot be negative");
            }
            while (size() > depth) {
                remove(size() - 1);
            }

        }

        public ContextStack copy() {
            return new ThreadContextStack(this);
        }
    }

    /**
     * An immutable ContextStack.
     */
    public static class ImmutableStack extends ThreadContextStack {

        private static final long serialVersionUID = 5050502L;

        public ImmutableStack() {
        }

        public ImmutableStack(Collection<String> collection) {
            super(collection);
        }

        public ImmutableStack(ThreadContextStack stack) {
            super(stack);
        }

        @Override
        public void push(String message) {
            throw new UnsupportedOperationException("Stack cannot be modified");
        }

        @Override
        public void trim(int depth) {
            throw new UnsupportedOperationException("Stack cannot be modified");
        }
    }

    /**
     * An immutable Context Map.
     */
    public static class ImmutableMap extends HashMap<String, String> {
        private static final long serialVersionUID = 5050503L;

        public ImmutableMap() {
            super();
        }

        public ImmutableMap(Map<String, String> map) {
            super(map);
        }

        @Override
        public String put(String s, String s1) {
            throw new UnsupportedOperationException("Map cannot be modified");
        }

        @Override
        public void putAll(Map<? extends String, ? extends String> map) {
            throw new UnsupportedOperationException("Map cannot be modified");
        }

        @Override
        public String remove(Object o) {
            throw new UnsupportedOperationException("Map cannot be modified");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Map cannot be modified");
        }
    }
}
