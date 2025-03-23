package com.airepublic.bmstoinverter.core.util;

import java.nio.ByteBuffer;

public class ByteAsciiConverter {

    public static byte convertAsciiBytesToByte(final byte high, final byte low) {
        final String ascii = new String(new char[] { (char) high, (char) low });
        return HexUtil.fromHexDigits(ascii);
    }


    public static byte[] convertByteToAsciiBytes(final byte value) {
        final byte[] bytes = new byte[2];
        final String byteStr = String.format("%02X", value);
        bytes[0] = (byte) byteStr.charAt(0);
        bytes[1] = (byte) byteStr.charAt(1);

        return bytes;
    }


    public static byte[] convertStringToAsciiBytes(final String value, final int noOfCharacters) {
        // create byte array (2 bytes per ascii char representation)
        final byte[] bytes = new byte[noOfCharacters * 2];
        int byteIdx = 0;
        int charIdx = 0;

        while (byteIdx < bytes.length) {
            // check if there are enough characters in the string
            if (charIdx < value.length()) {
                // translate the next character to ascii bytes
                final byte[] hex = convertByteToAsciiBytes((byte) value.charAt(charIdx++));
                bytes[byteIdx++] = hex[0];
                bytes[byteIdx++] = hex[1];
            } else {
                // otherwise fill it up with ascii bytes 0x30 (0x0)
                bytes[byteIdx++] = 0x30;
                bytes[byteIdx++] = 0x30;
            }
        }

        return bytes;
    }


    public static byte[] convertShortToAsciiBytes(final short value) {
        final byte first = (byte) ((value & 0xFF00) >> 8);
        final byte second = (byte) (value & 0x00FF);
        final byte[] data = new byte[4];
        System.arraycopy(convertByteToAsciiBytes(first), 0, data, 0, 2);
        System.arraycopy(convertByteToAsciiBytes(second), 0, data, 2, 2);

        return data;
    }


    public static void printAscii(final String str) {
        final String[] valueStr = str.split(" ");
        int i = 0;

        while (i < valueStr.length) {
            final byte high = HexUtil.fromHexDigits(valueStr[i++]);
            final byte low = HexUtil.fromHexDigits(valueStr[i++]);
            System.out.print("" + (char) convertAsciiBytesToByte(high, low));
        }

        System.out.println();
    }


    public static short convertAsciiBytesToShort(final byte[] value) {
        final byte first = convertAsciiBytesToByte(value[0], value[1]);
        final byte second = convertAsciiBytesToByte(value[2], value[3]);
        short result = first;
        result = (short) (result << 8 & second);

        return result;
    }


    public static String convertAsciiBytesToString(final ByteBuffer data, final int noOfCharacters) {
        // create byte array (2 bytes per ascii char representation)
        final byte[] asciiBytes = new byte[noOfCharacters * 2];
        data.get(asciiBytes);

        int asciiIdx = 0;
        final StringBuffer buf = new StringBuffer();

        while (asciiIdx < asciiBytes.length + 1) {
            final byte high = asciiBytes[asciiIdx++];
            final byte low = asciiBytes[asciiIdx++];
            final char chr = (char) convertAsciiBytesToByte(high, low);

            if (chr != 0) {
                buf.append(chr);
            }
        }

        return buf.toString();
    }

}
