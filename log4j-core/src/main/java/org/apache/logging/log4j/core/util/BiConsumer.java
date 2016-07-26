package org.apache.logging.log4j.core.util;

/**
 * An operation that accepts two input arguments and returns no result.
 *
 * @param <K> type of the first argument
 * @param <V> type of the second argument
 * @see org.apache.logging.log4j.core.ContextData
 * @since 2.7
 */
public interface BiConsumer<K, V> {

    /**
     * Performs the operation given the specified arguments.
     * @param k the first input argument
     * @param v the second input argument
     */
    void accept(K k, V v);
}
