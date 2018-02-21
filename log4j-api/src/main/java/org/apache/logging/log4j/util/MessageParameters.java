package org.apache.logging.log4j.util;

import java.util.Arrays;

/**
 * <em>Consider this class private.</em>
 */
public final class MessageParameters {
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private MessageParameters() {
        // Utility Class
    }

    /**
     * Gets a trimmed parameter array given a potentially over-sized array and current length.
     *
     * @param parameters     Original array, nullable
     * @param parameterCount number of parameters in use
     * @return an array of parameters sized to the number of parameters
     * present, or null if the parameters argument is null.
     */
    public static Object[] getParametersNullable(/* nullable */ Object[] parameters, int parameterCount) {
        if (parameters == null) {
            return null;
        }
        return parameterCount == 0 ? EMPTY_OBJECT_ARRAY : Arrays.copyOf(parameters, parameterCount);
    }

    /**
     * Gets a trimmed parameter array given a potentially over-sized array and current length.
     *
     * @param parameters     Original array, nullable
     * @param parameterCount number of parameters in use
     * @return an array of parameters sized to the number of parameters
     * present, or an empty array if the parameters argument is null.
     */
    public static Object[] getParameters(/* nullable */ Object[] parameters, int parameterCount) {
        return (parameterCount == 0 || parameters == null) ?
                EMPTY_OBJECT_ARRAY :
                Arrays.copyOf(parameters, parameterCount);
    }
}
