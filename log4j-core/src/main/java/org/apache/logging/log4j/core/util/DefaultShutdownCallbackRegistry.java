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
package org.apache.logging.log4j.core.util;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.LifeCycle2;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * ShutdownRegistrationStrategy that simply uses {@link Runtime#addShutdownHook(Thread)}. If no strategy is specified,
 * this one is used for shutdown hook registration.
 *
 * @since 2.1
 */
public class DefaultShutdownCallbackRegistry implements ShutdownCallbackRegistry, LifeCycle2, Runnable {
    /** Status logger. */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private final AtomicReference<State> state = new AtomicReference<>(State.INITIALIZED);
    private final ThreadFactory threadFactory;

    // use references to prevent memory leaks
    private final Collection<Reference<Cancellable>> hooks = new CopyOnWriteArrayList<>();
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
            for (final Reference<Cancellable> hookRef : hooks) {
                final Cancellable hook = hookRef.get();
                if (hook != null) {
                    try {
                        hook.run();
                    } catch (final Throwable t1) {
                        try {
                            LOGGER.error(SHUTDOWN_HOOK_MARKER, "Caught exception executing shutdown hook {}", hook, t1);
                        } catch (final Throwable t2) {
                            System.err.println(
                                    "Caught exception " + t2.getClass() + " logging exception " + t1.getClass());
                            t1.printStackTrace();
                        }
                    }
                }
            }
            state.set(State.STOPPED);
        }
    }

    private static class RegisteredCancellable implements Cancellable {
        private Runnable callback;
        private Collection<Reference<Cancellable>> registered;

        RegisteredCancellable(final Runnable callback, final Collection<Reference<Cancellable>> registered) {
            this.callback = callback;
            this.registered = registered;
        }

        @Override
        public void cancel() {
            callback = null;
            final Collection<Reference<Cancellable>> references = registered;
            if (references != null) {
                registered = null;
                references.removeIf(ref -> {
                    final Cancellable value = ref.get();
                    return value == null || value == RegisteredCancellable.this;
                });
            }
        }

        @Override
        public void run() {
            final Runnable runnableHook = callback;
            if (runnableHook != null) {
                runnableHook.run();
                callback = null;
            }
        }

        @Override
        public String toString() {
            return String.valueOf(callback);
        }
    }

    @Override
    public Cancellable addShutdownCallback(final Runnable callback) {
        if (isStarted()) {
            final Cancellable receipt = new RegisteredCancellable(callback, hooks);
            hooks.add(new SoftReference<>(receipt));
            return receipt;
        }
        throw new IllegalStateException("Cannot add new shutdown hook as this is not started. Current state: "
                + state.get().name());
    }

    @Override
    public void initialize() {}

    /**
     * Registers the shutdown thread only if this is initialized.
     */
    @Override
    public void start() {
        if (state.compareAndSet(State.INITIALIZED, State.STARTING)) {
            try {
                addShutdownHook(threadFactory.newThread(this));
                state.set(State.STARTED);
            } catch (final IllegalStateException ex) {
                state.set(State.STOPPED);
                throw ex;
            } catch (final Exception e) {
                LOGGER.catching(e);
                state.set(State.STOPPED);
            }
        }
    }

    private void addShutdownHook(final Thread thread) {
        shutdownHookRef = new WeakReference<>(thread);
        Runtime.getRuntime().addShutdownHook(thread);
    }

    @Override
    public void stop() {
        stop(AbstractLifeCycle.DEFAULT_STOP_TIMEOUT, AbstractLifeCycle.DEFAULT_STOP_TIMEUNIT);
    }

    /**
     * Cancels the shutdown thread only if this is started.
     */
    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        if (state.compareAndSet(State.STARTED, State.STOPPING)) {
            try {
                removeShutdownHook();
            } finally {
                state.set(State.STOPPED);
            }
        }
        return true;
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
