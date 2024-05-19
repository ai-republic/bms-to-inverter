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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

public class InstallationDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final JTextArea textArea;

    public InstallationDialog(final JFrame frame) {
        super(frame, "Installing...", true);
        setLocation(frame.getX() + frame.getBounds().width / 2 - 320, frame.getY() + frame.getBounds().height / 2 - 240);
        setSize(new Dimension(640, 480));
        getContentPane().setLayout(new BorderLayout(0, 0));

        final JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        getContentPane().add(panel);
        final GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.columnWidths = new int[] { 0 };
        gbl_panel.rowHeights = new int[] { 400, 30 };
        gbl_panel.columnWeights = new double[] { Double.MIN_VALUE };
        gbl_panel.rowWeights = new double[] { Double.MIN_VALUE };
        panel.setLayout(gbl_panel);

        final JScrollPane scrollPane = new JScrollPane();
        final GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 0;
        panel.add(scrollPane, gbc_scrollPane);

        textArea = new JTextArea();
        final DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        scrollPane.setViewportView(textArea);

        final JButton closeButton = new JButton("Close");
        closeButton.setHorizontalTextPosition(SwingConstants.CENTER);
        closeButton.setSize(new Dimension(90, 30));
        closeButton.setVerticalAlignment(SwingConstants.BOTTOM);
        final GridBagConstraints gbc_closeButton = new GridBagConstraints();
        gbc_closeButton.insets = new Insets(0, 0, 5, 0);
        gbc_closeButton.anchor = GridBagConstraints.NORTH;
        gbc_closeButton.gridx = 0;
        gbc_closeButton.gridy = 1;
        panel.add(closeButton, gbc_closeButton);
        closeButton.addActionListener(e -> dispose());
    }


    public void startInstallation(final Runnable installer) throws Throwable {
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        final Future<?> future = executor.schedule(installer, 1, TimeUnit.SECONDS);
        setVisible(true);

        try {
            future.get();
        } catch (final InterruptedException e) {
            throw e.getCause();
        } catch (final ExecutionException e) {
            throw e.getCause();
        } finally {
            executor.shutdown();
        }
    }


    public JTextArea getTextArea() {
        return textArea;
    }

}
