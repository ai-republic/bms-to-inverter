package com.airepublic.bmstoinverter.bms.jk.rs485;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to resemble a whole RS485 response frame.
 */
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

    /**
     * Constructor.
     * 
     * @param buffer the received bytes representing a complete response
     */
    public JKBmsRS485ResponseFrame(final byte[] buffer) {
        dataEntries = new ArrayList<>();
        size = buffer.length;

        dataEntries = new ArrayList<>();
        parse(buffer);

    }


    /**
     * Parse the bytes that represent a complete response frame.
     *
     * @param buffer the response frame bytes
     */
    private void parse(final byte[] buffer) {
        final ByteBuffer wrappedBuffer = ByteBuffer.wrap(buffer);

        // start flag
        stx = new byte[2];
        wrappedBuffer.get(stx);

        // length of the rest of the frame including this length
        length = new byte[2];
        wrappedBuffer.get(length);

        // terminal number
        terminalNumber = new byte[4];
        wrappedBuffer.get(terminalNumber);

        // command
        commandWord = wrappedBuffer.get();
        // source
        frameSource = wrappedBuffer.get();
        // request/response
        transportType = wrappedBuffer.get();

        // read the frame except the last 9 status/checksum bytes
        while (wrappedBuffer.position() < size - 9) {
            // check if the remaining buffer starts with a valid data id
            final byte dataId = wrappedBuffer.get();
            final boolean foundDataId = JkBmsR485DataIdEnum.isDataId(dataId);

            if (foundDataId) {
                final var dataEntry = new DataEntry();
                // get the data id for the entry
                final var dataIdType = JkBmsR485DataIdEnum.fromDataId(dataId);
                dataEntry.setId(dataIdType);

                // get the length of the data segment
                int length = dataIdType.getLength();

                // special handling for cell voltages,
                if (dataIdType.equals(JkBmsR485DataIdEnum.READ_CELL_VOLTAGES)) {
                    // the first data byte declares the number bytes for all cells
                    length = wrappedBuffer.get();
                }

                // copy the relevant data bytes and set them for this entry
                final var datacopy = new byte[length];
                wrappedBuffer.get(datacopy);
                dataEntry.setData(ByteBuffer.wrap(datacopy));
                dataEntries.add(dataEntry);
            } else {
                wrappedBuffer.position(size - 9);
            }
        }

        recordNumber = new byte[4];
        wrappedBuffer.get(recordNumber);
        endIdentity = wrappedBuffer.get();
        checksum = new byte[4];
        wrappedBuffer.get(checksum);

    }

    public static class DataEntry {
        private JkBmsR485DataIdEnum id;
        private ByteBuffer data;

        /**
         * @return the id
         */
        public JkBmsR485DataIdEnum getId() {
            return id;
        }


        /**
         * @param id the id to set
         */
        public void setId(final JkBmsR485DataIdEnum id) {
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