package com.airepublic.bmstoinverter.protocol.rs485;

public enum FrameDefinitionPartType {
    START_FLAG('S'),
    COMMAND('C'),
    LENGTH('L'),
    ADDRESS('A'),
    DATA('D'),
    CHECKSUM('V'),
    OTHER('O');

    private final char letter;

    FrameDefinitionPartType(final char letter) {
        this.letter = letter;
    }


    public char getLetter() {
        return letter;
    }


    public static FrameDefinitionPartType valueOf(final char chr) {
        switch (chr) {
            case 'S':
                return START_FLAG;
            case 'C':
                return COMMAND;
            case 'L':
                return LENGTH;
            case 'A':
                return ADDRESS;
            case 'D':
                return DATA;
            case 'V':
                return CHECKSUM;
            case 'O':
                return OTHER;
            default:
                return null;
        }
    }
}