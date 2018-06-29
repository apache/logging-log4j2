package org.apache.logging.log4j.message;

/**
 * {@link Clearable} objects may be reset to a reusable state.
 *
 * This type should be folded into {@link ReusableMessage} for 3.0.
 *
 * @since 2.11.1
 */
public interface Clearable {

    /**
     * Resets the object to a clean state.
     */
    void clear();

}
