package org.apache.logging.log4j.core;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * An extensible {@link LifeCycle} using an {@link AtomicReference} to wrap its {@link LifeCycle.State}. Thus, classes
 * which extend this class will follow the finite state machine as follows:
 * <ol>
 * <li>When {@link #start()} is called, {@link #doStart()} is called if and only if this is in the INITIALIZED state or
 * the STOPPED state.</li>
 * <li>Before {@link #doStart()} is called, this will be in the STARTING state.</li>
 * <li>After {@link #doStart()} is called, this will be in the STARTED state if no exception was thrown; otherwise,
 * this will be in the INITIALIZED state again, and the exception thrown will be re-thrown (if unchecked) or wrapped
 * in an {@link java.lang.reflect.UndeclaredThrowableException} and then rethrown (if checked).</li>
 * <li>When {@link #stop()} is called, {@link #doStop()} is called if and only if this is in the STARTED state.</li>
 * <li>Before {@link #doStop()} is called, this will be in the STOPPING state.</li>
 * <li>After {@link #doStop()} is called, this will be in the STOPPED state. Any exceptions thrown will be re-thrown
 * as described above.</li>
 * </ol>
 *
 * @since 2.1
 */
public abstract class AbstractAtomicLifeCycle implements LifeCycle, Serializable {

    private static final long serialVersionUID = 1L;

    protected static final StatusLogger LOGGER = StatusLogger.getLogger();

    private final AtomicReference<State> state = new AtomicReference<State>(State.INITIALIZED);

    @Override
    public void start() {
        if (state.compareAndSet(State.INITIALIZED, State.STARTING) ||
            state.compareAndSet(State.STOPPED, State.STARTING)) {
            try {
                doStart();
                state.set(State.STARTED);
            } catch (final Exception e) {
                state.set(State.INITIALIZED);
                Throwables.rethrow(e);
            }
        }
    }

    /**
     * Performs the start-up logic. This method is called only if this is in the INITIALIZED or STOPPED state.
     *
     * @throws Exception
     */
    protected abstract void doStart() throws Exception;

    @Override
    public void stop() {
        if (state.compareAndSet(State.STARTED, State.STOPPING)) {
            try {
                doStop();
            } catch (Exception e) {
                Throwables.rethrow(e);
            } finally {
                state.set(State.STOPPED);
            }
        }
    }

    /**
     * Performs the tear-down logic. This method is called only if this is in the STARTED state.
     *
     * @throws Exception
     */
    protected abstract void doStop() throws Exception;

    @Override
    public boolean isStarted() {
        return state.get() == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state.get() == State.STOPPED;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AbstractAtomicLifeCycle that = (AbstractAtomicLifeCycle) o;
        return state.equals(that.state);
    }

    @Override
    public int hashCode() {
        return state.hashCode();
    }
}
