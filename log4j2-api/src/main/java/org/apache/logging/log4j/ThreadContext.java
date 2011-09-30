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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


/**
 * The ThreadContext allows applications to store information either in a Map
 * <p/>
 * <p><b><em>The MDC is managed on a per thread basis</em></b>. A
 * child thread automatically inherits a <em>copy</em> of the mapped
 * diagnostic context of its parent.
 */
public final class ThreadContext {

    private static ThreadLocal<Map<String, String>> localMap =
        new InheritableThreadLocal<Map<String, String>>() {
            protected Map<String, String> initialValue() {
                return new HashMap<String, String>();
            }

            protected Map<String, String> childValue(Map<String, String> parentValue) {
                return parentValue == null ? new HashMap<String, String>() : new HashMap<String, String>(parentValue);
            }
        };

    private static ThreadLocal<Stack<String>> localStack =
        new InheritableThreadLocal<Stack<String>>() {
            protected Stack<String> initialValue() {
                return new Stack<String>();
            }

            protected Stack<String> childValue(Stack<String> parentValue) {
                return parentValue == null ? null : (Stack<String>) parentValue.clone();
            }
        };



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
        localMap.get().put(key, value);
    }

    /**
     * Get the context identified by the <code>key</code> parameter.
     * <p/>
     * <p>This method has no side effects.
     * @param key The key to locate.
     * @return The value associated with the key or null.
     */
    public static String get(String key) {
        return localMap.get().get(key);
    }

    /**
     * Remove the the context identified by the <code>key</code>
     * parameter.
     * @param key The key to remove.
     */
    public static void remove(String key) {
        localMap.get().remove(key);
    }

    /**
     * Clear the context.
     */
    public static void clear() {
        localMap.get().clear();
    }

    /**
     * Determine if the key is in the context.
     * @param key The key to locate.
     * @return True if the key is in the context, false otherwise.
     */
    public static boolean containsKey(String key) {
        return localMap.get().containsKey(key);
    }

    /**
     * Get the current thread's MDC as a hashtable. This method is
     * intended to be used internally.
     * @return a copy of the context.
     */
    public static Map<String, Object> getContext() {
        return new HashMap<String, Object>(localMap.get());
    }

    /**
     * Clear the stack for this thread.
     */
    public static void clearStack() {
        localStack.get().clear();
    }

    /**
     * Return a copy of this thread's stack.
     * @return A copy of this thread's stack.
     */
    public static Stack<String> cloneStack() {
        return (Stack<String>) localStack.get().clone();
    }

    /**
     * Set this thread's stack.
     * @param stack The stack to use.
     */
    public static void setStack(Stack<String> stack) {
        localStack.set(stack);
    }

    /**
     * Get the current nesting depth of this thread's stack.
     * @return the number of items in the stack.
     *
     * @see #setMaxDepth
     */
    public static int getDepth() {
        return localStack.get().size();
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
        Stack<String> s = localStack.get();
        if (s.isEmpty()) {
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
         Stack<String> s = localStack.get();
        if (s.isEmpty()) {
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
        localStack.get().push(message);
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
     * Set maximum depth of this diagnostic context. If the current
     * depth is smaller or equal to <code>maxDepth</code>, then no
     * action is taken.
     * <p/>
     * <p>This method is a convenient alternative to multiple {@link
     * #pop} calls. Moreover, it is often the case that at the end of
     * complex call sequences, the depth of the NDC is
     * unpredictable. The <code>setMaxDepth</code> method circumvents
     * this problem.
     * <p/>
     * <p>For example, the combination
     * <pre>
     * void foo() {
     * &nbsp;  int depth = NDC.getDepth();
     * <p/>
     * &nbsp;  ... complex sequence of calls
     * <p/>
     * &nbsp;  NDC.setMaxDepth(depth);
     * }
     * </pre>
     * <p/>
     * ensures that between the entry and exit of foo the depth of the
     * diagnostic stack is conserved.
     *
     * @see #getDepth
     * @param maxDepth The maximum depth of the stack.
     */
    public static void setMaxDepth(int maxDepth) {

    }
}
