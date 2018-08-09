package org.apache.logging.log4j.message;

import org.apache.logging.log4j.spi.AbstractLogger;

public class ReusableParameterizedMessageContentFormatter implements MessageContentFormatter {
    private static final ThreadLocal<int[]> localIndices = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[256];
        }
    };

    @Override
    public void formatTo(String formatString, Object[] parameters, int parameterCount, StringBuilder buffer) {
        // in the event that a parameter's toString generates a log message,
        // avoids clobbering indices that were computed from the initial call
        // see also LOG4J2-1583
        if (AbstractLogger.getRecursionDepth() > 1) {
            ParameterFormatter.formatMessage(buffer, formatString, parameters, parameterCount);
            return;
        }

        int[] indices = localIndices.get();
        int placeholderCount = ReusableParameterizedMessage.count(formatString, indices);
        int usedCount = Math.min(placeholderCount, parameterCount);

        if (indices[0] < 0) {
            ParameterFormatter.formatMessage(buffer, formatString, parameters, parameterCount);
        } else {
            ParameterFormatter.formatMessage2(buffer, formatString, parameters, usedCount, indices);
        }
    }
}
