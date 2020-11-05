package org.apache.logging.log4j.util;

public enum SneakyThrow {;

    /**
     * Throws any exception (including checked ones!) without defining it in the method signature.
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(final Throwable throwable) throws E {
        throw (E) throwable;
    }

}
