package com.airepublic.bmstoinverter.configurator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

public class Configurator extends JFrame {
    private static final long serialVersionUID = 1L;
    private final GeneralPanel generalPanel;
    private final BMSPanel bmsPanel;
    private final InverterPanel inverterPanel;
    private final ServicesPanel servicesPanel;

    public Configurator() {
        super("BMS-to-Inverter Configurator");
        setSize(new Dimension(600, 524));
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout(10, 10));

        final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
        getContentPane().add(tabbedPane, BorderLayout.NORTH);

        generalPanel = new GeneralPanel();
        tabbedPane.addTab("General", null, generalPanel, null);

        bmsPanel = new BMSPanel(this);
        tabbedPane.addTab("BMS", bmsPanel);

        inverterPanel = new InverterPanel();
        tabbedPane.addTab("Inverter", null, inverterPanel, null);

        final JScrollPane servicesScrollPane = new JScrollPane();
        tabbedPane.addTab("Services", null, servicesScrollPane, null);

        servicesPanel = new ServicesPanel();
        servicesScrollPane.setViewportView(servicesPanel);

        final JPanel buttonPanel = new JPanel();
        final GridBagLayout gbl_buttonPanel = new GridBagLayout();
        gbl_buttonPanel.columnWidths = new int[] { 0, 120, 120 };
        gbl_buttonPanel.rowHeights = new int[] { 0, 50 };
        gbl_buttonPanel.columnWeights = new double[] { 0.0, 0.0, 0.0 };
        gbl_buttonPanel.rowWeights = new double[] { 0.0, 0.0 };
        buttonPanel.setLayout(gbl_buttonPanel);

        final JButton createButton = new JButton("Create");
        final GridBagConstraints gbc_createButton = new GridBagConstraints();
        gbc_createButton.anchor = GridBagConstraints.EAST;
        gbc_createButton.insets = new Insets(0, 0, 0, 5);
        gbc_createButton.gridx = 1;
        gbc_createButton.gridy = 1;
        buttonPanel.add(createButton, gbc_createButton);
        createButton.addActionListener(e -> {
            try {
                generateConfiguration();
            } catch (final IOException e1) {
                e1.printStackTrace();
            }
        });

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());
        final GridBagConstraints gbc_cancelButton = new GridBagConstraints();
        gbc_cancelButton.anchor = GridBagConstraints.WEST;
        gbc_cancelButton.gridx = 2;
        gbc_cancelButton.gridy = 1;
        buttonPanel.add(cancelButton, gbc_cancelButton);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }


    public boolean verify(final StringBuffer errors) {
        boolean fail = false;

        if (!generalPanel.verify(errors)) {
            fail = true;
        }

        if (!bmsPanel.verify(errors)) {
            fail = true;
        }

        if (!inverterPanel.verify(errors)) {
            fail = true;
        }

        if (!servicesPanel.verify(errors)) {
            fail = true;
        }

        return !fail;
    }


    private void generateConfiguration() throws IOException {
        final StringBuffer errors = new StringBuffer();
        if (!verify(errors)) {
            JOptionPane.showInternalMessageDialog(getContentPane(), errors, "Error", JOptionPane.ERROR_MESSAGE);
            System.out.println(errors);
            return;
        }

        final StringBuffer config = new StringBuffer();
        config.append("###################################################################\n"
                + "###                  System specific settings                   ###\n"
                + "###################################################################\n"
                + "\n");
        bmsPanel.generateConfiguration(config);
        inverterPanel.generateConfiguration(config);
        servicesPanel.generateConfiguration(config);

        buildApplication(config.toString());
    }


    private void buildApplication(final String config) {
        System.out.println(config);

        try {
            // define paths
            final Path installDirectory = Path.of(generalPanel.getInstallationPath());
            System.out.println("Installing in: " + installDirectory);
            final Path configDirectory = installDirectory.resolve("config");
            System.out.println("Configuration in: " + configDirectory);
            final Path tempDirectory = installDirectory.resolve("temp");
            System.out.println("Temp directory is: " + tempDirectory);
            final Path srcDirectory = tempDirectory.resolve("bms-to-inverter-main");
            final Path srcZip = tempDirectory.resolve("bms-to-inverter.zip");
            final Path mavenDirectory = tempDirectory.resolve("apache-maven-3.9.6");
            final Path mavenZip = tempDirectory.resolve("maven.zip");

            // clean up previous directories
            if (Files.exists(srcDirectory)) {
                Files.walk(srcDirectory)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            // create directories
            Files.createDirectories(installDirectory);
            Files.createDirectories(tempDirectory);

            // check if previous maven is present
            if (!Files.exists(mavenDirectory)) {
                System.out.print("Downloading maven...");
                downloadFile(new URL("https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"), mavenZip.toFile());
                System.out.println("done");
                unzip(mavenZip, tempDirectory);
                Files.delete(mavenZip);
            }

            // download the application source
            System.out.print("Downloading application...");
            downloadFile(new URL("https://github.com/ai-republic/bms-to-inverter/archive/master.zip"), srcZip.toFile());
            System.out.println("done");
            unzip(srcZip, tempDirectory);

            // build the application
            System.out.print("Building application...");
            final String command = mavenDirectory.toString() + "/bin/mvn clean package -DskipTests=true";
            final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            final ProcessBuilder builder = new ProcessBuilder();
            builder.directory(srcDirectory.toFile());
            builder.redirectErrorStream(true);

            if (isWindows) {
                builder.command("cmd.exe", "/c", command);
            } else {
                builder.command("sh", "-c", command);
            }

            final Process process = builder.start();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            final int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.out.println("FAILED");
            } else {
                System.out.println("SUCCESS");
            }

            // unzip generated application
            unzip(srcDirectory.resolve("bms-to-inverter-main/target/bms-to-inverter.zip"), installDirectory);

            // generate the configuration files
            Files.deleteIfExists(srcDirectory.resolve("bms-to-inverter-main/src/main/resources/config.properties"));
            Files.write(installDirectory.resolve("config.properties"), config.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

            // generate start scripts
            final StringBuffer commands = new StringBuffer("java -jar lib/bms-to-inverter-main-0.0.1-SNAPSHOT.jar -DconfigFile=config.properties\n");

            if (servicesPanel.isWebserverEnabled()) {
                commands.append("java -jar lib/webserver-0.0.1-SNAPSHOT.jar --spring.config.location=file://config.properties\n");
            }

            final Path windowsStart = Path.of("start.cmd");
            Files.deleteIfExists(windowsStart);
            Files.createFile(windowsStart);
            Files.write(windowsStart, commands.toString().getBytes());

            final Path linuxStart = Path.of("start");
            Files.deleteIfExists(windowsStart);
            Files.createFile(windowsStart);
            Files.write(linuxStart, ("#!/bin/bash\n" + commands.toString()).getBytes());

        } catch (final Exception e) {
            System.out.println("Installation FAILED!");
            e.printStackTrace();
        }
    }


    boolean downloadFile(final URL url, final File file) throws IOException {
        final ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
        final FileOutputStream fileOutputStream = new FileOutputStream(file);
        final FileChannel fileChannel = fileOutputStream.getChannel();
        fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        fileOutputStream.close();
        return true;
    }


    void unzip(final Path fileZip, final Path destDir) throws Exception {

        final byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip.toFile()))) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                final File newFile = newFile(destDir.toFile(), zipEntry);

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // fix for Windows-created archives
                    final File parent = newFile.getParentFile();

                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    final FileOutputStream fos = new FileOutputStream(newFile);
                    int len;

                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }

                    fos.close();
                }

                zipEntry = zis.getNextEntry();
            }

            zis.closeEntry();
        }
    }


    File newFile(final File destinationDir, final ZipEntry zipEntry) throws IOException {
        final File destFile = new File(destinationDir, zipEntry.getName());

        final String destDirPath = destinationDir.getCanonicalPath();
        final String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }


    public static void main(final String[] args) {
        new Configurator();
    }

}
