package org.apache.logging.log4j.core;

/**
 *
 */
public interface Layout {
    // Note that the line.separator property can be looked up even by
    // applets.
    public static final String LINE_SEP = System.getProperty("line.separator");
    public static final int LINE_SEP_LEN = LINE_SEP.length();

    /**
     * Formats the event suitable for display.
     * @param event The Logging Event.
     * @return The formatted event String.
     */
    byte[] format(LogEvent event);

    /**
     * Returns the header for the layout format.
     * @return The header.
     */
    byte[] getHeader();

    /**
     * Returns the format for the layout format.
     * @return The footer.
     */
    byte[] getFooter();


}
