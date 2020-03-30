package org.apache.logging.log4j.message;

/**
 * An operation that accepts two input arguments and returns no result.
 *
 * <p>
 * The third parameter lets callers pass in a stateful object to be modified with the key-value pairs,
 * so the ParameterConsumer implementation itself can be stateless and potentially reusable.
 * </p>
 *
 * @param <S> state data
 * @see ReusableMessage
 * @since 2.11
 */
public interface ParameterConsumer<S> {

    /**
     * Performs an operation given the specified arguments.
     *
     * @param parameter the parameter
     * @param parameterIndex Index of the parameter
     * @param state The state data.
     */
    void accept(Object parameter, int parameterIndex, S state);

}
