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
