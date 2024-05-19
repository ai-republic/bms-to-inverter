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
package com.airepublic.bmstoinverter.configurator;

import java.util.regex.Pattern;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

public class NumberInputVerifier extends InputVerifier {
    private final Pattern pattern = Pattern.compile("-?\\d+?");

    @Override
    public boolean verify(final JComponent input) {
        return verify(((JTextField) input).getText());
        // try {
        // Long.valueOf(text);
        // input.setBackground(Color.WHITE);
        // return true;
        // } catch (final NumberFormatException e) {
        // input.setBackground(Color.RED.brighter().brighter().brighter());
        // return false;
        // }
    }


    public boolean verify(final String text) {
        return pattern.matcher(text).matches();
    }
}
