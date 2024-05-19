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
package com.airepublic.bmstoinverter.core.protocol.rs485;

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
