package org.apache.logging.log4j.core.config;

import java.util.Comparator;

import org.apache.logging.log4j.core.util.Assert;

/**
 * Comparator for classes annotated with {@link Order}.
 *
 * @since 2.1
 */
public class OrderComparator implements Comparator<Class<?>> {
    @Override
    public int compare(final Class<?> lhs, final Class<?> rhs) {
        final Order lhsOrder = Assert.requireNonNull(lhs, "lhs").getAnnotation(Order.class);
        final Order rhsOrder = Assert.requireNonNull(rhs, "rhs").getAnnotation(Order.class);
        if (lhsOrder == null && rhsOrder == null) {
            // both unannotated means equal priority
            return 0;
        }
        // if only one class is @Order-annotated, then prefer that one
        if (rhsOrder == null) {
            return -1;
        }
        if (lhsOrder == null) {
            return 1;
        }
        // larger value means lower priority
        return Integer.signum(rhsOrder.value() - lhsOrder.value());
    }
}
