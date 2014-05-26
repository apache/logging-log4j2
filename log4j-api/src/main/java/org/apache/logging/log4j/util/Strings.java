package org.apache.logging.log4j.util;

/**
 * <em>Consider this class private.</em>
 */
public final class Strings {

    /**
     * The empty string.
     */
    public static final String EMPTY = "";

    private Strings() {
    }

    /**
     * <p>Checks if a CharSequence is empty ("") or null.</p>
     *
     * <pre>
     * Strings.isEmpty(null)      = true
     * Strings.isEmpty("")        = true
     * Strings.isEmpty(" ")       = false
     * Strings.isEmpty("bob")     = false
     * Strings.isEmpty("  bob  ") = false
     * </pre>
     *
     * <p>NOTE: This method changed in Lang version 2.0.
     * It no longer trims the CharSequence.
     * That functionality is available in isBlank().</p>
     *
     * <p>Copied from Apache Commons Lang org.apache.commons.lang3.StringUtils.isEmpty(CharSequence)</p>
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * <p>Checks if a CharSequence is not empty ("") and not null.</p>
     *
     * <pre>
     * Strings.isNotEmpty(null)      = false
     * Strings.isNotEmpty("")        = false
     * Strings.isNotEmpty(" ")       = true
     * Strings.isNotEmpty("bob")     = true
     * Strings.isNotEmpty("  bob  ") = true
     * </pre>
     *
     * <p>Copied from Apache Commons Lang org.apache.commons.lang3.StringUtils.isNotEmpty(CharSequence)</p>
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }

    /**
     * <p>Removes control characters (char &lt;= 32) from both
     * ends of this String returning {@code null} if the String is
     * empty ("") after the trim or if it is {@code null}.
     *
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.</p>
     *
     * <pre>
     * Strings.trimToNull(null)          = null
     * Strings.trimToNull("")            = null
     * Strings.trimToNull("     ")       = null
     * Strings.trimToNull("abc")         = "abc"
     * Strings.trimToNull("    abc    ") = "abc"
     * </pre>
     *
     * <p>Copied from Apache Commons Lang org.apache.commons.lang3.StringUtils.trimToNull(String)</p>
     *
     * @param str  the String to be trimmed, may be null
     * @return the trimmed String,
     *  {@code null} if only chars &lt;= 32, empty or null String input
     */
    public static String trimToNull(final String str) {
        final String ts = str == null ? null : str.trim();
        return isEmpty(ts) ? null : ts;
    }

}
