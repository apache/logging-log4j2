package org.apache.logging.log4j.core.config.plugins.convert;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility methods for Date classes.
 */
public final class DateTypeConverter {

    private static final Map<Class<? extends Date>, MethodHandle> CONSTRUCTORS = new ConcurrentHashMap<>();

    static {
        final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        for (final Class<? extends Date> dateClass : Arrays.asList(Date.class, java.sql.Date.class, Time.class,
            Timestamp.class)) {
            try {
                CONSTRUCTORS.put(dateClass,
                    lookup.findConstructor(dateClass, MethodType.methodType(void.class, long.class)));
            } catch (final NoSuchMethodException | IllegalAccessException ignored) {
                // these classes all have this exact constructor
            }
        }
    }

    /**
     * Create a Date-related object from a timestamp in millis.
     *
     * @param millis timestamp in millis
     * @param type   date type to use
     * @param <D>    date class to use
     * @return new instance of D or null if there was an error
     */
    @SuppressWarnings("unchecked")
    public static <D extends Date> D fromMillis(final long millis, final Class<D> type) {
        try {
            return (D) CONSTRUCTORS.get(type).invoke(millis);
        } catch (final Throwable ignored) {
            return null;
        }
    }

    private DateTypeConverter() {
    }
}
