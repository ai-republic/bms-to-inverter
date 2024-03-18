package com.airepublic.bmstoinverter.bms.jk.rs485;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
    private final int size;

    public JKBmsRS485ResponseFrame(final byte[] buffer) {
        dataEntries = new ArrayList<>();
        size = buffer.length;

        dataEntries = new ArrayList<>();
        parse(buffer);

    }


    private void parse(final byte[] buffer) {
        final ByteBuffer wrappedBuffer = ByteBuffer.wrap(buffer);
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

        while (index < size - 9) {
            final boolean foundDataId = JkBmsR485DataIdEnum.dataId(wrappedBuffer.get(index));

            if (foundDataId) {
                final var dataEntry = new DataEntry();
                dataEntry.setId(wrappedBuffer.get(index));
                final var dataIdType = JkBmsR485DataIdEnum.fromDataId(wrappedBuffer.get(index));
                int length = dataIdType.getlength();
                if (dataIdType.equals(JkBmsR485DataIdEnum.READ_CELL_VOLTAGES)) {
                    length = buffer[index + 1] + 1;
                }
                if (length == 0) {
                    start = index + 1;
                    end = index + 1;

                    while (buffer[end] == 0 || !JkBmsR485DataIdEnum.dataId(wrappedBuffer.get(end)) && end < size - 9) {
                        end++;
                    }

                } else {
                    start = index + 1;
                    end = start + length;
                }
                final var datacopy = new byte[end - start];
                wrappedBuffer.get(start, datacopy);
                dataEntry.setData(ByteBuffer.wrap(datacopy));
                dataEntries.add(dataEntry);
                index = end;
            } else {
                index = size - 9;
            }

        }

        recordNumber = new byte[4];
        wrappedBuffer.get(index += 4, recordNumber);
        endIdentity = wrappedBuffer.get(index++);
        checksum = new byte[4];
        wrappedBuffer.get(index, checksum);

    }

    public static class DataEntry {
        private byte id;
        private ByteBuffer data;

        /**
         * @return the id
         */
        public byte getId() {
            return id;
        }


        /**
         * @param id the id to set
         */
        public void setId(final byte id) {
            this.id = id;
        }


        /**
         * @return the data
         */
        public ByteBuffer getData() {
            return data;
        }


        /**
         * @param data the data to set
         */
        public void setData(final ByteBuffer data) {
            this.data = data;
        }

    }

    /**
     * @return the stx
     */
    public byte[] getStx() {
        return stx;
    }


    /**
     * @param stx the stx to set
     */
    public void setStx(final byte[] stx) {
        this.stx = stx;
    }


    /**
     * @return the length
     */
    public byte[] getLength() {
        return length;
    }


    /**
     * @param length the length to set
     */
    public void setLength(final byte[] length) {
        this.length = length;
    }


    /**
     * @return the terminalNumber
     */
    public byte[] getTerminalNumber() {
        return terminalNumber;
    }


    /**
     * @param terminalNumber the terminalNumber to set
     */
    public void setTerminalNumber(final byte[] terminalNumber) {
        this.terminalNumber = terminalNumber;
    }


    /**
     * @return the commandWord
     */
    public byte getCommandWord() {
        return commandWord;
    }


    /**
     * @param commandWord the commandWord to set
     */
    public void setCommandWord(final byte commandWord) {
        this.commandWord = commandWord;
    }


    /**
     * @return the frameSource
     */
    public byte getFrameSource() {
        return frameSource;
    }


    /**
     * @param frameSource the frameSource to set
     */
    public void setFrameSource(final byte frameSource) {
        this.frameSource = frameSource;
    }


    /**
     * @return the transportType
     */
    public byte getTransportType() {
        return transportType;
    }


    /**
     * @param transportType the transportType to set
     */
    public void setTransportType(final byte transportType) {
        this.transportType = transportType;
    }


    /**
     * @return the dataEntries
     */
    public List<DataEntry> getDataEntries() {
        return dataEntries;
    }


    /**
     * @param dataEntries the dataEntries to set
     */
    public void setDataEntries(final List<DataEntry> dataEntries) {
        this.dataEntries = dataEntries;
    }


    /**
     * @return the recordNumber
     */
    public byte[] getRecordNumber() {
        return recordNumber;
    }


    /**
     * @param recordNumber the recordNumber to set
     */
    public void setRecordNumber(final byte[] recordNumber) {
        this.recordNumber = recordNumber;
    }


    /**
     * @return the endIdentity
     */
    public byte getEndIdentity() {
        return endIdentity;
    }


    /**
     * @param endIdentity the endIdentity to set
     */
    public void setEndIdentity(final byte endIdentity) {
        this.endIdentity = endIdentity;
    }


    /**
     * @return the checksum
     */
    public byte[] getChecksum() {
        return checksum;
    }


    /**
     * @param checksum the checksum to set
     */
    public void setChecksum(final byte[] checksum) {
        this.checksum = checksum;
    }


    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

}