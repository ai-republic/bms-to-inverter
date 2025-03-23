package com.airepublic.bmstoinverter.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamUtil {
    public static byte[] readAllBytes(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] data = new byte[1024];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
