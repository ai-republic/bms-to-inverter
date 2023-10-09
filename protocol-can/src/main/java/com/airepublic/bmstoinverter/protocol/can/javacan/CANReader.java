package com.airepublic.bmstoinverter.protocol.can.javacan;

import java.nio.ByteBuffer;

import com.airepublic.bmstoinverter.core.Port;

public class CANReader {

    public static void main(final String[] args) {
        try (JavaCANPort port = new JavaCANPort()) {
            port.setPortname("can0");

            // open CAN port
            try {
                System.out.println("Opening " + port.getPortname() + " ...");
                port.open();
                System.out.println("Opening CAN port SUCCESSFUL");

            } catch (final Throwable e) {
                System.err.println("Opening port FAILED!");
                e.printStackTrace();
            }

            do {
                final ByteBuffer receiveFrame = port.receiveFrame(bytes -> true);
                System.out.println("RECEIVED: " + Port.printBuffer(receiveFrame));

            } while (true);
        } catch (final Exception e) {
        }
    }
}
