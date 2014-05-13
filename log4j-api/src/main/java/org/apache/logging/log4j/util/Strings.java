package org.apache.logging.log4j.util;

/**
 * <em>Consider this class private.</em>
 */
public class Strings {

    /**
     * The empty string.
     */
    public static final String EMPTY = "";

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
     * StringUtils.isNotEmpty(null)      = false
     * StringUtils.isNotEmpty("")        = false
     * StringUtils.isNotEmpty(" ")       = true
     * StringUtils.isNotEmpty("bob")     = true
     * StringUtils.isNotEmpty("  bob  ") = true
     * </pre>
     *
     * <p>Copied from Apache Commons Lang org.apache.commons.lang3.StringUtils.isNotEmpty(CharSequence)</p>
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not null
     * @since 3.0 Changed signature from isNotEmpty(String) to isNotEmpty(CharSequence)
     */
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }
}
