package com.airepublic.bmstoinverter.core.util;

public class HexUtil {
    public static byte fromHexDigits(final String hex) {
        return (byte) Integer.parseInt(hex, 16);
    }


    public static String toHexDigits(final byte value) {
        return String.format("%02x", value);
    }


    public static String toHexDigits(final int value) {
        return toHexDigits((byte) value);
    }


    public static String formatHex(final byte[] data) {
        final StringBuilder hexString = new StringBuilder();
        boolean first = true;
        for (final byte b : data) {
            if (!first) {
                hexString.append(" ");
            }
            hexString.append(String.format("%02x", b));
            first = false;
        }

        return hexString.toString();
    }
}
