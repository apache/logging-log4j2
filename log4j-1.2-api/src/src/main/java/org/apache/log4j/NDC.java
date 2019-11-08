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
package org.apache.log4j;

import java.util.Stack;

/**
 *
 */
public final class NDC {

    private NDC() {
    }

    /**
     * Clear any nested diagnostic information if any. This method is
     * useful in cases where the same thread can be potentially used
     * over and over in different unrelated contexts.
     * <p>
     * This method is equivalent to calling the {@link #setMaxDepth}
     * method with a zero <code>maxDepth</code> argument.
     * </p>
     */
    public static void clear() {
        org.apache.logging.log4j.ThreadContext.clearStack();
    }


    /**
     * Clone the diagnostic context for the current thread.
     * <p>
     * Internally a diagnostic context is represented as a stack.  A
     * given thread can supply the stack (i.e. diagnostic context) to a
     * child thread so that the child can inherit the parent thread's
     * diagnostic context.
     * </p>
     * <p>
     * The child thread uses the {@link #inherit inherit} method to
     * inherit the parent's diagnostic context.
     * </p>
     * @return Stack A clone of the current thread's  diagnostic context.
     */
    @SuppressWarnings("rawtypes")
    public static Stack cloneStack() {
        final Stack<String> stack = new Stack<>();
        for (final String element : org.apache.logging.log4j.ThreadContext.cloneStack().asList()) {
            stack.push(element);
        }
        return stack;
    }


    /**
     * Inherit the diagnostic context of another thread.
     * <p>
     * The parent thread can obtain a reference to its diagnostic
     * context using the {@link #cloneStack} method.  It should
     * communicate this information to its child so that it may inherit
     * the parent's diagnostic context.
     * </p>
     * <p>
     * The parent's diagnostic context is cloned before being
     * inherited. In other words, once inherited, the two diagnostic
     * contexts can be managed independently.
     * </p>
     * <p>
     * In java, a child thread cannot obtain a reference to its
     * parent, unless it is directly handed the reference. Consequently,
     * there is no client-transparent way of inheriting diagnostic
     * contexts. Do you know any solution to this problem?
     * </p>
     * @param stack The diagnostic context of the parent thread.
     */
    public static void inherit(final Stack<String> stack) {
        org.apache.logging.log4j.ThreadContext.setStack(stack);
    }


    /**
     * <strong style="color:#FF4040">Never use this method directly.</strong>
     *
     * @return The string value of the specified key.
     */
    public static String get() {
        return org.apache.logging.log4j.ThreadContext.peek();
    }

    /**
     * Get the current nesting depth of this diagnostic context.
     * @return int The number of elements in the call stack.
     * @see #setMaxDepth
     */
    public static int getDepth() {
        return org.apache.logging.log4j.ThreadContext.getDepth();
    }

    /**
     * Clients should call this method before leaving a diagnostic
     * context.
     * <p>
     * The returned value is the value that was pushed last. If no
     * context is available, then the empty string "" is returned.
     * </p>
     * @return String The innermost diagnostic context.
     */
    public static String pop() {
        return org.apache.logging.log4j.ThreadContext.pop();
    }

    /**
     * Looks at the last diagnostic context at the top of this NDC
     * without removing it.
     * <p>
     * The returned value is the value that was pushed last. If no
     * context is available, then the empty string "" is returned.
     * </p>
     * @return String The innermost diagnostic context.
     */
    public static String peek() {
        return org.apache.logging.log4j.ThreadContext.peek();
    }

    /**
     * Push new diagnostic context information for the current thread.
     * <p>
     * The contents of the <code>message</code> parameter is
     * determined solely by the client.
     * </p>
     * @param message The new diagnostic context information.
     */
    public static void push(final String message) {
        org.apache.logging.log4j.ThreadContext.push(message);
    }

    /**
     * Remove the diagnostic context for this thread.
     * <p>
     * Each thread that created a diagnostic context by calling
     * {@link #push} should call this method before exiting. Otherwise,
     * the memory used by the <b>thread</b> cannot be reclaimed by the
     * VM.
     * </p>
     * <p>
     * As this is such an important problem in heavy duty systems and
     * because it is difficult to always guarantee that the remove
     * method is called before exiting a thread, this method has been
     * augmented to lazily remove references to dead threads. In
     * practice, this means that you can be a little sloppy and
     * occasionally forget to call {@code remove} before exiting a
     * thread. However, you must call <code>remove</code> sometime. If
     * you never call it, then your application is sure to run out of
     * memory.
     * </p>
     */
    public static void remove() {
        org.apache.logging.log4j.ThreadContext.removeStack();
    }

    /**
     * Set maximum depth of this diagnostic context. If the current
     * depth is smaller or equal to <code>maxDepth</code>, then no
     * action is taken.
     * <p>
     * This method is a convenient alternative to multiple {@link
     * #pop} calls. Moreover, it is often the case that at the end of
     * complex call sequences, the depth of the NDC is
     * unpredictable. The <code>setMaxDepth</code> method circumvents
     * this problem.
     * </p>
     * <p>
     * For example, the combination
     * </p>
     * <pre>
     * void foo() {
     * &nbsp;  int depth = NDC.getDepth();
     *
     * &nbsp;  ... complex sequence of calls
     *
     * &nbsp;  NDC.setMaxDepth(depth);
     * }
     * </pre>
     * <p>
     * ensures that between the entry and exit of foo the depth of the
     * diagnostic stack is conserved.
     * </p>
     *
     * @see #getDepth
     * @param maxDepth The maximum depth of the stack.
     */
    public static void setMaxDepth(final int maxDepth) {
        org.apache.logging.log4j.ThreadContext.trim(maxDepth);
    }
}
