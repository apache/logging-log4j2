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

package org.apache.logging.log4j.core.util;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LifeCycle.State;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * ShutdownRegistrationStrategy that simply uses {@link Runtime#addShutdownHook(Thread)}. If no strategy is specified,
 * this one is used for shutdown hook registration.
 *
 * @since 2.1
 */
public class DefaultShutdownCallbackRegistry implements ShutdownCallbackRegistry, LifeCycle, Runnable, Serializable {

    private static final long serialVersionUID = 1L;
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private final AtomicReference<State> state = new AtomicReference<State>(State.INITIALIZED);
    private final ThreadFactory threadFactory;
    private final Collection<Cancellable> hooks = new CopyOnWriteArrayList<Cancellable>();
    private Reference<Thread> shutdownHookRef;

    /**
     * Constructs a DefaultShutdownRegistrationStrategy.
     */
    public DefaultShutdownCallbackRegistry() {
        this(Executors.defaultThreadFactory());
    }

    /**
     * Constructs a DefaultShutdownRegistrationStrategy using the given {@link ThreadFactory}.
     *
     * @param threadFactory the ThreadFactory to use to create a {@link Runtime} shutdown hook thread
     */
    protected DefaultShutdownCallbackRegistry(final ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    /**
     * Executes the registered shutdown callbacks.
     */
    @Override
    public void run() {
        if (state.compareAndSet(State.STARTED, State.STOPPING)) {
            for (final Runnable hook : hooks) {
                try {
                    hook.run();
                } catch (final Throwable t) {
                    LOGGER.error(SHUTDOWN_HOOK_MARKER, "Caught exception executing shutdown hook {}", hook, t);
                }
            }
            state.set(State.STOPPED);
        }
    }

    @Override
    public Cancellable addShutdownCallback(final Runnable callback) {
        if (isStarted()) {
            final Cancellable receipt = new Cancellable() {
                // use a reference to prevent memory leaks
                private final Reference<Runnable> hook = new SoftReference<Runnable>(callback);

                @Override
                public void cancel() {
                    hook.clear();
                    hooks.remove(this);
                }

                @Override
                public void run() {
                    final Runnable hook = this.hook.get();
                    if (hook != null) {
                        hook.run();
                        this.hook.clear();
                    }
                }

                @Override
                public String toString() {
                    return String.valueOf(hook.get());
                }
            };
            hooks.add(receipt);
            return receipt;
        }
        throw new IllegalStateException("Cannot add new shutdown hook as this is not started. Current state: " +
            state.get().name());
    }

    /**
     * Registers the shutdown thread only if this is initialized.
     */
    @Override
    public void start() {
        if (state.compareAndSet(State.INITIALIZED, State.STARTING)) {
            try {
                addShutdownHook(threadFactory.newThread(this));
                state.set(State.STARTED);
            } catch (final Exception e) {
                LOGGER.catching(e);
                state.set(State.STOPPED);
            }
        }
    }

    private void addShutdownHook(final Thread thread) {
        shutdownHookRef = new WeakReference<Thread>(thread);
        Runtime.getRuntime().addShutdownHook(thread);
    }

    /**
     * Cancels the shutdown thread only if this is started.
     */
    @Override
    public void stop() {
        if (state.compareAndSet(State.STARTED, State.STOPPING)) {
            try {
                removeShutdownHook();
            } finally {
                state.set(State.STOPPED);
            }
        }
    }

    private void removeShutdownHook() {
        final Thread shutdownThread = shutdownHookRef.get();
        if (shutdownThread != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownThread);
            shutdownHookRef.enqueue();
        }
    }

    @Override
    public State getState() {
        return state.get();
    }

    /**
     * Indicates if this can accept shutdown hooks.
     *
     * @return true if this can accept shutdown hooks
     */
    @Override
    public boolean isStarted() {
        return state.get() == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state.get() == State.STOPPED;
    }

}
