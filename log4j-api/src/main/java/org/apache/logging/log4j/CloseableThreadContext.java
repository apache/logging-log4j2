package org.apache.logging.log4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Adds entries to the {@link ThreadContext stack or map} and them removes them when the object is closed, e.g. as part
 * of a try-with-resources.
 * 
 * @since 2.6
 */
public class CloseableThreadContext implements AutoCloseable {

    /**
     * Pushes new diagnostic context information on to the Thread Context Stack. The information will be popped off when
     * the instance is closed.
     * 
     * @param message
     *            The new diagnostic context information.
     * @return a new instance that will back out the changes when closed.
     */
    public static CloseableThreadContext push(final String message) {
        return new CloseableThreadContext(message);
    }

    /**
     * Pushes new diagnostic context information on to the Thread Context Stack. The information will be popped off when
     * the instance is closed.
     * 
     * @param message
     *            The new diagnostic context information.
     * @param args
     *            Parameters for the message.
     * @return a new instance that will back out the changes when closed.
     */
    public static CloseableThreadContext push(final String message, final Object... args) {
        return new CloseableThreadContext(message, args);
    }

    /**
     * Populates the Thread Context Map with the supplied key/value pairs. Any existing keys in the
     * {@link ThreadContext} will be replaced with the supplied values, and restored back to their original values when
     * the instance is closed.
     *
     * @param firstKey
     *            The first key to be added
     * @param firstValue
     *            The first value to be added
     * @param subsequentKeyValuePairs
     *            Any subsequent key/value pairs to be added. Note: If the last key does not have a corresponding value
     *            then an empty String will be used as a value.
     * @return a new instance that will back out the changes when closed.
     */
    public static CloseableThreadContext put(final String firstKey, final String firstValue,
            final String... subsequentKeyValuePairs) {
        return new CloseableThreadContext(firstKey, firstValue, subsequentKeyValuePairs);
    }

    private final boolean isStack;

    private final Map<String, String> oldValues = new HashMap<>();

    /**
     * Creates an instance of a {@code CloseableThreadContext} that pushes new diagnostic context information on to the
     * Thread Context Stack. The information will be popped off when the instance is closed.
     * 
     * @param message
     *            The new diagnostic context information.
     */
    public CloseableThreadContext(final String message) {
        this.isStack = true;
        ThreadContext.push(message);
    }

    /**
     * Creates an instance of a {@code CloseableThreadContext} that pushes new diagnostic context information on to the
     * Thread Context Stack. The information will be popped off when the instance is closed.
     * 
     * @param message
     *            The new diagnostic context information.
     * @param args
     *            Parameters for the message.
     */
    public CloseableThreadContext(final String message, final Object... args) {
        this.isStack = true;
        ThreadContext.push(message, args);
    }

    /**
     * Creates an instance of a {@code CloseableThreadContext} that populates the Thread Context Map with the supplied
     * key/value pairs. Any existing keys in the ThreadContext will be replaced with the supplied values, and restored
     * back to their original values when the instance is closed.
     *
     * @param firstKey
     *            The first key to be added
     * @param firstValue
     *            The first value to be added
     * @param subsequentKeyValuePairs
     *            Any subsequent key/value pairs to be added. Note: If the last key does not have a corresponding value
     *            then an empty String will be used as a value.
     */
    public CloseableThreadContext(final String firstKey, final String firstValue,
            final String... subsequentKeyValuePairs) {
        this.isStack = false;
        storeAndSet(firstKey, firstValue);
        for (int i = 0; i < subsequentKeyValuePairs.length; i += 2) {
            final String key = subsequentKeyValuePairs[i];
            final String value = (i + 1) < subsequentKeyValuePairs.length ? subsequentKeyValuePairs[i + 1] : "";
            storeAndSet(key, value);
        }
    }

    /**
     * Removes the values from the {@link ThreadContext}.
     * <p>
     * If this {@code CloseableThreadContext} was added to the {@link ThreadContext} <em>stack</em>, then this will pop
     * the diagnostic information off the stack.
     * </p>
     * <p>
     * If the {@code CloseableThreadContext} was added to the {@link ThreadContext} <em>map</em>, then this will either
     * remove the values that were added, or restore them to their original values it they already existed.
     * </p>
     */
    @Override
    public void close() {
        if (this.isStack) {
            closeStack();
        } else {
            closeMap();
        }
    }

    private void closeMap() {
        for (final Map.Entry<String, String> entry : oldValues.entrySet()) {
            // If the old value was null, remove it from the ThreadContext
            if (null == entry.getValue()) {
                ThreadContext.remove(entry.getKey());
            } else {
                ThreadContext.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private String closeStack() {
        return ThreadContext.pop();
    }

    private void storeAndSet(final String key, final String value) {
        // If there are no existing values, a null will be stored as an old value
        oldValues.put(key, ThreadContext.get(key));
        ThreadContext.put(key, value);
    }

}
