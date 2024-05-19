/**
 * This software is free to use and to distribute in its unchanged form for private use.
 * Commercial use is prohibited without an explicit license agreement of the copyright holder.
 * Any changes to this software must be made solely in the project repository at https://github.com/ai-republic/bms-to-inverter.
 * The copyright holder is not liable for any damages in whatever form that may occur by using this software.
 *
 * (c) Copyright 2022 and onwards - Torsten Oltmanns
 *
 * @author Torsten Oltmanns - bms-to-inverter''AT''gmail.com
 */
package com.airepublic.bmstoinverter.core.util;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A class to read bytes from a queue.
 */
public class ByteReaderWriter implements AutoCloseable {
    private final ConcurrentLinkedDeque<byte[]> queue = new ConcurrentLinkedDeque<>();

    /**
     * Reads bytes from the available queue into the specified array. If there are not enough bytes
     * in the queue to fill the array it will return -1. Otherwise the byte array will be filled,
     * the queue adjusted and the byte array length.
     *
     * @param bytes the bytes to be read
     * @return the byte array length or -1
     */
    public synchronized int read(final byte[] bytes) {
        synchronized (queue) {
            final byte[] first = queue.peek();

            // check if there are any bytes available
            if (first == null) {
                return -1;
            }

            // determine how many byte array elements are needed to fill the requested byte array
            int elementsNeeded = 0;
            int byteCount = 0;

            for (final byte[] element : queue) {
                byteCount += element.length;
                elementsNeeded++;

                if (byteCount >= bytes.length) {
                    break;
                }
            }

            // if not enough bytes are available
            if (byteCount < bytes.length) {
                // return fault
                return -1;
            }

            int remaining = bytes.length;

            for (int i = 0; i < elementsNeeded; i++) {
                final byte[] element = queue.peek();

                if (element.length <= remaining) {
                    // copy all of the elements bytes into the requested byte array
                    System.arraycopy(element, 0, bytes, bytes.length - remaining, element.length);
                    // remove the head and adjust the remaining bytes to read
                    queue.pop();
                    remaining -= element.length;
                } else {
                    // copy the first part of the element to fill the requested bytes
                    System.arraycopy(element, 0, bytes, bytes.length - remaining, remaining);

                    // now create a new head element to replace the current head
                    final byte[] head = new byte[element.length - remaining];
                    // copy the remaining element bytes to the new head
                    System.arraycopy(element, remaining, head, 0, head.length);
                    // exchange the new head
                    queue.pop();
                    queue.addFirst(head);

                    // no more bytes to read
                    remaining = 0;
                }
            }
        }

        return bytes.length;
    }


    /**
     * Gets one byte from the queue and adjusts the queue.
     *
     * @return the next byte from the queue
     * @throws IOException if no bytes are available
     */
    public int read() throws IOException {
        synchronized (queue) {
            final byte[] head = queue.peek();

            if (head == null) {
                throw new IOException("No bytes available!");
            }

            if (head.length > 1) {
                final byte[] newHead = new byte[head.length - 1];

                // copy the remaining element bytes to the new head
                System.arraycopy(head, 1, newHead, 0, newHead.length);
                // exchange the new head
                queue.pop();
                queue.addFirst(newHead);
            } else {
                queue.pop();
            }

            return head[0];
        }
    }


    /**
     * Adds the specified bytes to the tail of the queue.
     *
     * @param bytes the bytes to add
     */
    public void write(final byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }

        synchronized (queue) {
            queue.addLast(bytes);
        }
    }


    /**
     * Clears the underlying queue.
     */
    public void clear() {
        queue.clear();
    }


    @Override
    public void close() throws Exception {
        queue.clear();
    }
}
