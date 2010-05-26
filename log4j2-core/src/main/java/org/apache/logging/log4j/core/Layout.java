package org.apache.logging.log4j.core;

/**
 * @doubt There is still a need for a character-based layout for character based event sinks (databases, etc).
 *  Would introduce an EventEncoder, EventRenderer or something similar for the logging event to byte encoding.
 */
public interface Layout {
    // Note that the line.separator property can be looked up even by
    // applets.
    /**
     * @doubt It is very conceivable that distinct layouts might use distinct line separators.  Should not be on the interface.
     */
    public static final String LINE_SEP = System.getProperty("line.separator");
    public static final int LINE_SEP_LEN = LINE_SEP.length();

    /**
     * Formats the event suitable for display.
     * @param event The Logging Event.
     * @return The formatted event.
     * @doubt Likely better to write to a OutputStream instead of return a byte[].
     */
    byte[] format(LogEvent event);

    /**
     * Returns the header for the layout format.
     * @return The header.
     * @doubt the concept of header and footer is not universal, should not be on the base interface.
     */
    byte[] getHeader();

    /**
     * Returns the format for the layout format.
     * @return The footer.
     * @doubt the concept of header and footer is not universal, should not be on the base interface.
     */
    byte[] getFooter();


}
