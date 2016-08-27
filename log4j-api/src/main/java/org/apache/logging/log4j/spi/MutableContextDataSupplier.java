package org.apache.logging.log4j.spi;

/**
 * Interface for objects that know how to provide a {@code MutableContextData} object.
 *
 * @since 2.7
 */
public interface MutableContextDataSupplier {

    /**
     * Retuns the {@code MutableContextData}.
     * @return the {@code MutableContextData}
     */
    MutableContextData getMutableContextData();
}
