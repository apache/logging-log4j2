package org.apache.logging.log4j.core.config.plugins.convert;

/**
 * Converts Strings to hex. This is used in place of java.xml.bind.DataTypeConverter which is not available by
 * default in Java 9.
 */
public class HexConverter {

    public static byte[] parseHexBinary(final String s) {
        final int len = s.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
