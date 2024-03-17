package com.airepublic.bmstoinverter.bms.jk.rs485;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Data
public class JKBmsRS485ResponseFrame {
    private byte[] stx; // Start Frame
    private byte[] length; // LENGTH
    private byte[] terminalNumber; // Terminal number
    private byte commandWord; // Command word
    private byte frameSource; // Frame Source
    private byte transportType; // Transport type
    private List<DataEntry> dataEntries; // X-times (ID + Data)
    private byte[] recordNumber; // Record number
    private byte endIdentity; // End Identity
    private byte[] checksum; // Checksum
    private int size;

    public JKBmsRS485ResponseFrame(byte[] buffer) {
        this.dataEntries = new ArrayList<>();
        this.size = buffer.length;

        dataEntries = new ArrayList<>();
        parse(buffer);

    }

    private void parse(byte[] buffer) {
        ByteBuffer wrappedBuffer = ByteBuffer.wrap(buffer);
        var index = 0;
        var start = 0;
        var end = 0;
        stx = new byte[2];
        wrappedBuffer.get(index += 2, stx);

        length = new byte[2];
        wrappedBuffer.get(index += 2, length);
        terminalNumber = new byte[4];
        wrappedBuffer.get(index += 4, terminalNumber);
        commandWord = wrappedBuffer.get(index++);
        frameSource = wrappedBuffer.get(index++);
        transportType = wrappedBuffer.get(index++);

        while (index < this.size - 9) {
            boolean foundDataId = JkBmsR485DataIdEnum.dataId(wrappedBuffer.get(index));

            if (foundDataId) {
                var dataEntry = new DataEntry();
                dataEntry.setId(wrappedBuffer.get(index));
                var dataIdType = JkBmsR485DataIdEnum.fromDataId(wrappedBuffer.get(index));
                int length = dataIdType.getlength();
                if (dataIdType.equals(JkBmsR485DataIdEnum.READ_CELL_VOLTAGES)) {
                    length = buffer[index + 1] + 1;
                }
                if (length == 0) {
                    start = index+1;
                    end = index + 1;


                    while (buffer[end]==0 || (!JkBmsR485DataIdEnum.dataId(wrappedBuffer.get(end)) && end < this.size - 9)) {
                        end++;
                    }


                } else {
                    start = index + 1;
                    end = start + length;
                }
                var datacopy = new byte[end - start];
                wrappedBuffer.get(start, datacopy);
                dataEntry.setData(ByteBuffer.wrap(datacopy));
                dataEntries.add(dataEntry);
                index = end ;
            } else {
                index = this.size - 9;
            }


        }


        this.recordNumber = new byte[4];
        wrappedBuffer.get(index += 4, recordNumber);
        this.endIdentity = wrappedBuffer.get(index++);
        this.checksum = new byte[4];
        wrappedBuffer.get(index, checksum);


    }

    @Data
    @NoArgsConstructor
    public static class DataEntry {
        private byte id;
        private ByteBuffer data;

    }

}