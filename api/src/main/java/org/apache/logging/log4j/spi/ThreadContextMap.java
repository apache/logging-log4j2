package org.apache.logging.log4j.spi;

import java.util.Map;

/**
 *
 */
public interface ThreadContextMap {
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
    void put(final String key, final String value);

    /**
     * Get the context identified by the <code>key</code> parameter.
     * <p/>
     * <p>This method has no side effects.
     * @param key The key to locate.
     * @return The value associated with the key or null.
     */
    String get(final String key);

    /**
     * Remove the the context identified by the <code>key</code>
     * parameter.
     * @param key The key to remove.
     */
    void remove(final String key);

    /**
     * Clear the context.
     */
    void clear();

    /**
     * Determine if the key is in the context.
     * @param key The key to locate.
     * @return True if the key is in the context, false otherwise.
     */
    boolean containsKey(final String key);

    /**
     * Get a copy of current thread's context Map.
     * @return a copy of the context.
     */
    Map<String, String> getContext();

    /**
     * Return the actual context Map.
     * @return the actual context Map.
     */
    Map<String, String> get();

    /**
     * Returns true if the Map is empty.
     * @return true if the Map is empty, false otherwise.
     */
    boolean isEmpty();
}
