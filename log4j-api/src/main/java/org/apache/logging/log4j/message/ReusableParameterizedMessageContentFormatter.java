package org.apache.logging.log4j.message;

public class ReusableParameterizedMessageContentFormatter implements MessageContentFormatter {
    private static final ThreadLocal<int[]> localIndices = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[256];
        }
    };

    @Override
    public void formatTo(String formatString, Object[] parameters, int parameterCount, StringBuilder buffer) {
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
