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

import org.apache.logging.log4j.internal.StatusLogger;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The NDC class implements <i>nested diagnostic contexts</i> as
 * defined by Neil Harrison in the article "Patterns for Logging
 * Diagnostic Messages" part of the book "<i>Pattern Languages of
 * Program Design 3</i>" edited by Martin et al.
 * <p/>
 * <p>A Nested Diagnostic Context, or NDC in short, is an instrument
 * to distinguish interleaved log output from different sources. Log
 * output is typically interleaved when a server handles multiple
 * clients near-simultaneously.
 * <p/>
 * <p>Interleaved log output can still be meaningful if each log entry
 * from different contexts had a distinctive stamp. This is where NDCs
 * come into play.
 * <p/>
 * <p><em><b>Note that NDCs are managed on a per thread
 * basis</b></em>. NDC operations such as {@link #push push}, {@link
 * #pop}, {@link #clear}, {@link #getDepth} and {@link #setMaxDepth}
 * affect the NDC of the <em>current</em> thread only. NDCs of other
 * threads remain unaffected.
 * <p/>
 * <p>For example, a servlet can build a per client request NDC
 * consisting the clients host name and other information contained in
 * the the request. <em>Cookies</em> are another source of distinctive
 * information. To build an NDC one uses the {@link #push push}
 * operation. Simply put,
 * <p/>
 * <p><ul>
 * <li>Contexts can be nested.
 * <p/>
 * <p><li>When entering a context, call <code>NDC.push</code>. As a
 * side effect, if there is no nested diagnostic context for the
 * current thread, this method will create it.
 * <p/>
 * <p><li>When leaving a context, call <code>NDC.pop</code>.
 * <p/>
 * <p><li><b>When exiting a thread make sure to call {@link #remove
 * NDC.remove()}</b>.
 * </ul>
 * <p/>
 * <p>There is no penalty for forgetting to match each
 * <code>push</code> operation with a corresponding <code>pop</code>,
 * except the obvious mismatch between the real application context
 * and the context set in the NDC.
 * <p/>
 * <p>Heavy duty systems should call the {@link #remove} method when
 * leaving the run method of a thread. This ensures that the memory
 * used by the thread can be freed by the Java garbage
 * collector. There is a mechanism to lazily remove references to dead
 * threads. In practice, this means that you can be a little sloppy
 * and sometimes forget to call {@link #remove} before exiting a
 * thread.
 * <p/>
 * <p>A thread may inherit the nested diagnostic context of another
 * (possibly parent) thread using the {@link #inherit inherit}
 * method. A thread may obtain a copy of its NDC with the {@link
 * #cloneStack cloneStack} method and pass the reference to any other
 * thread, in particular to a child.
 *  @doubt I'd combine MDC and NDC into a single ThreadContext object. (RG) To be
 * honest, I've always doubted the practicality of this class. An errant
 * app that forgets to pop an element will screw it up. I only added it
 * because Log4J 1.x supports it and it wasn't particularly hard to carry over.
 */
public class NDC {

    // The synchronized keyword is not used in this class. This may seem
    // dangerous, especially since the class will be used by
    // multiple-threads. In particular, all threads share the same
    // hashtable (the "ht" variable). This is OK since java hashtables
    // are thread safe. Same goes for Stacks.

    // More importantly, when inheriting diagnostic contexts the child
    // thread is handed a clone of the parent's NDC.  It follows that
    // each thread has its own NDC (i.e. stack).

    private static Logger logger = StatusLogger.getLogger();

    static ConcurrentMap<Thread, Stack<Object>> ht = new ConcurrentHashMap<Thread, Stack<Object>>();

    static int pushCounter = 0; // the number of times push has been called
    // after the latest call to lazyRemove

    // The number of times we allow push to be called before we call lazyRemove
    // 5 is a relatively small number. As such, lazyRemove is not called too
    // frequently. We thus avoid the cost of creating an Enumeration too often.
    // The higher this number, the longer is the avarage period for which all
    // logging calls in all threads are blocked.
    static final int REAP_THRESHOLD = 5;

    // No instances allowed.

    private NDC() {
    }

    /**
     * Get NDC stack for current thread.
     *
     * @return NDC stack for current thread.
     */
    private static Stack<Object> getCurrentStack() {
        if (ht != null) {
            return (Stack<Object>) ht.get(Thread.currentThread());
        }
        return null;
    }


    /**
     * Clear any nested diagnostic information if any. This method is
     * useful in cases where the same thread can be potentially used
     * over and over in different unrelated contexts.
     * <p/>
     * <p>This method is equivalent to calling the {@link #setMaxDepth}
     * method with a zero <code>maxDepth</code> argument.
     *
     * @since 0.8.4c
     */
    public static void clear() {
        Stack stack = getCurrentStack();
        if (stack != null) {
            stack.setSize(0);
        }
    }


    /**
     * Clone the diagnostic context for the current thread.
     * <p/>
     * <p>Internally a diagnostic context is represented as a stack.  A
     * given thread can supply the stack (i.e. diagnostic context) to a
     * child thread so that the child can inherit the parent thread's
     * diagnostic context.
     * <p/>
     * <p>The child thread uses the {@link #inherit inherit} method to
     * inherit the parent's diagnostic context.
     *
     * @return Stack A clone of the current thread's  diagnostic context.
     */
    public static Stack<Object> cloneStack() {
        Stack stack = getCurrentStack();
        if (stack == null) {
            return null;
        } else {
            return (Stack<Object>) stack.clone();
        }
    }


    /**
     * Inherit the diagnostic context of another thread.
     * <p/>
     * <p>The parent thread can obtain a reference to its diagnostic
     * context using the {@link #cloneStack} method.  It should
     * communicate this information to its child so that it may inherit
     * the parent's diagnostic context.
     * <p/>
     * <p>The parent's diagnostic context is cloned before being
     * inherited. In other words, once inherited, the two diagnostic
     * contexts can be managed independently.
     * <p/>
     * <p>In java, a child thread cannot obtain a reference to its
     * parent, unless it is directly handed the reference. Consequently,
     * there is no client-transparent way of inheriting diagnostic
     * contexts. Do you know any solution to this problem?
     *
     * @param stack The diagnostic context of the parent thread.
     */
    public static void inherit(Stack<Object> stack) {
        if (stack != null) {
            ht.put(Thread.currentThread(), stack);
        }
    }


    /**
     * <font color="#FF4040"><b>Never use this method directly.
     */
    public static String get() {
        Stack<Object> s = getCurrentStack();
        if (s != null && !s.isEmpty()) {
            return ((DiagnosticContext) s.peek()).fullMessage;
        } else {
            return null;
        }
    }

    /**
     * Get the current nesting depth of this diagnostic context.
     *
     * @see #setMaxDepth
     * @since 0.7.5
     */
    public static int getDepth() {
        Stack<Object> stack = getCurrentStack();
        if (stack == null) {
            return 0;
        } else {
            return stack.size();
        }
    }

    private static void lazyRemove() {
        if (ht == null) {
            return;
        }

        Vector<Thread> v;

        // Avoid calling clean-up too often.
        if (++pushCounter <= REAP_THRESHOLD) {
            return; // We release the lock ASAP.
        } else {
            pushCounter = 0; // OK let's do some work.
        }

        int misses = 0;
        v = new Vector<Thread>();
        Iterator<Thread> iter = ht.keySet().iterator();
        // We give up after 4 straight missses. That is 4 consecutive
        // inspected threads in 'ht' that turn out to be alive.
        // The higher the proportion on dead threads in ht, the higher the
        // chances of removal.
        while (iter.hasNext() && (misses <= 4)) {
            Thread t = iter.next();
            if (t.isAlive()) {
                misses++;
            } else {
                misses = 0;
                v.addElement(t);
            }
        }

        int size = v.size();
        for (int i = 0; i < size; i++) {
            Thread t = v.elementAt(i);
            logger.debug("Lazy NDC removal for thread [{}] ({}).", t.getName(), ht.size());
            ht.remove(t);
        }
    }

    /**
     * Clients should call this method before leaving a diagnostic
     * context.
     * <p/>
     * <p>The returned value is the value that was pushed last. If no
     * context is available, then the empty string "" is returned.
     *
     * @return String The innermost diagnostic context.
     */
    public static String pop() {
        Stack<Object> stack = getCurrentStack();
        if (stack != null && !stack.isEmpty()) {
            return ((DiagnosticContext) stack.pop()).message;
        } else {
            return "";
        }
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
        Stack<Object> stack = getCurrentStack();
        if (stack != null && !stack.isEmpty()) {
            return ((DiagnosticContext) stack.peek()).message;
        } else {
            return "";
        }
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
        Stack<Object> stack = getCurrentStack();

        if (stack == null) {
            DiagnosticContext dc = new DiagnosticContext(message, null);
            stack = new Stack<Object>();
            Thread key = Thread.currentThread();
            ht.put(key, stack);
            stack.push(dc);
        } else if (stack.isEmpty()) {
            DiagnosticContext dc = new DiagnosticContext(message, null);
            stack.push(dc);
        } else {
            DiagnosticContext parent = (DiagnosticContext) stack.peek();
            stack.push(new DiagnosticContext(message, parent));
        }
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
    public static void remove() {
        if (ht != null) {
            ht.remove(Thread.currentThread());

            // Lazily remove dead-thread references in ht.
            lazyRemove();
        }
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
        Stack<Object> stack = getCurrentStack();
        if (stack != null && maxDepth < stack.size()) {
            stack.setSize(maxDepth);
        }
    }

    // =====================================================================

    private static class DiagnosticContext {

        String fullMessage;
        String message;

        DiagnosticContext(String message, DiagnosticContext parent) {
            this.message = message;
            if (parent != null) {
                fullMessage = parent.fullMessage + ' ' + message;
            } else {
                fullMessage = message;
            }
        }
    }
}
