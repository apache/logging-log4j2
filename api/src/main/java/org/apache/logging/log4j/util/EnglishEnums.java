package org.apache.logging.log4j.util;

import java.util.Locale;

/**
 * Helps convert English Strings to English Enum values.
 * <p>
 * Enum name arguments are converted internally to upper case with the {@linkplain Locale#ENGLISH ENGLISH} locale to
 * avoid problems on the Turkish locale. Do not use with Turkish enum values.
 * </p>
 */
public class EnglishEnums {

    /**
     * Returns the Result for the given string.
     * <p>
     * The {@code name} is converted internally to upper case with the {@linkplain Locale#ENGLISH ENGLISH} locale to
     * avoid problems on the Turkish locale. Do not use with Turkish enum values.
     * </p>
     * 
     * @param name
     *            The enum name, case-insensitive. If null, returns {@code defaultValue}
     * @return an enum value or null if {@code name} is null
     */
    public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {
        return valueOf(enumType, name, null);
    }

    /**
     * Returns an enum value for the given string.
     * <p>
     * The {@code name} is converted internally to upper case with the {@linkplain Locale#ENGLISH ENGLISH} locale to
     * avoid problems on the Turkish locale. Do not use with Turkish enum values.
     * </p>
     * 
     * @param name
     *            The enum name, case-insensitive. If null, returns {@code defaultValue}
     * @param defaultValue
     *            the enum value to return if {@code name} is null
     * @return an enum value or {@code defaultValue} if {@code name} is null
     */
    public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name, T defaultValue) {
        return name == null ? defaultValue : Enum.valueOf(enumType, name.toUpperCase(Locale.ENGLISH));
    }

}
