package org.apache.logging.log4j.message;

import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Allows message parameters to be iterated over without any allocation
 * or memory copies.
 *
 * @since 2.11
 */
@PerformanceSensitive("allocation")
public interface ParameterVisitable {

    /**
     * Performs the given action for each parameter until all values
     * have been processed or the action throws an exception.
     * <p>
     * The second parameter lets callers pass in a stateful object to be modified with the key-value pairs,
     * so the TriConsumer implementation itself can be stateless and potentially reusable.
     * </p>
     *
     * @param action The action to be performed for each key-value pair in this collection
     * @param state the object to be passed as the third parameter to each invocation on the
     *          specified ParameterConsumer.
     * @param <S> type of the third parameter
     * @since 2.11
     */
    <S> void forEachParameter(ParameterConsumer<S> action, S state);

}
