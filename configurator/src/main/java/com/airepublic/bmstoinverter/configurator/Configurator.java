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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.airepublic.bmstoinverter.core.util.InputStreamUtil;

public class Configurator extends JFrame {
    private static final long serialVersionUID = 1L;
    private final GeneralPanel generalPanel;
    private final JButton updateConfigButton;
    private final BMSPanel bmsPanel;
    private final InverterPanel inverterPanel;
    private final ServicesPanel servicesPanel;
    private final PluginsPanel pluginsPanel;

    public Configurator() {
        super("BMS-to-Inverter Configurator");
        setSize(new Dimension(600, 524));
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout(10, 10));

        final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
        getContentPane().add(tabbedPane, BorderLayout.NORTH);

        generalPanel = new GeneralPanel(this);
        tabbedPane.addTab("General", null, generalPanel, null);

        bmsPanel = new BMSPanel(this);
        tabbedPane.addTab("BMS", bmsPanel);

        inverterPanel = new InverterPanel(this);
        tabbedPane.addTab("Inverter", null, inverterPanel, null);

        pluginsPanel = new PluginsPanel(this);
        tabbedPane.addTab("Plugins", null, pluginsPanel, null);

        final JScrollPane servicesScrollPane = new JScrollPane();
        tabbedPane.addTab("Services", null, servicesScrollPane, null);

        servicesPanel = new ServicesPanel(this);
        servicesScrollPane.setViewportView(servicesPanel);

        final JPanel buttonPanel = new JPanel();
        final GridBagLayout gbl_buttonPanel = new GridBagLayout();
        gbl_buttonPanel.columnWidths = new int[] { 0, 0, 120, 120 };
        gbl_buttonPanel.rowHeights = new int[] { 0, 50 };
        gbl_buttonPanel.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
        gbl_buttonPanel.rowWeights = new double[] { 0.0, 0.0 };
        buttonPanel.setLayout(gbl_buttonPanel);

        updateConfigButton = new JButton("Update Configuration");
        final GridBagConstraints gbc_updateConfigButton = new GridBagConstraints();
        gbc_updateConfigButton.insets = new Insets(0, 0, 0, 5);
        gbc_updateConfigButton.gridx = 1;
        gbc_updateConfigButton.gridy = 1;
        buttonPanel.add(updateConfigButton, gbc_updateConfigButton);
        disableUpdateConfiguration();
        updateConfigButton.addActionListener(e -> {
            try {
                updateConfiguration();
                JOptionPane.showMessageDialog(Configurator.this, "Successfully updated the configuration!", "Information", JOptionPane.INFORMATION_MESSAGE);
            } catch (final Exception e1) {
                JOptionPane.showMessageDialog(Configurator.this, "Failed to update the configuration!\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e1.printStackTrace();
            }
        });

        final JButton installButton = new JButton("Clean install");
        final GridBagConstraints gbc_installButton = new GridBagConstraints();
        gbc_installButton.insets = new Insets(0, 0, 0, 5);
        gbc_installButton.gridx = 2;
        gbc_installButton.gridy = 1;
        buttonPanel.add(installButton, gbc_installButton);
        installButton.addActionListener(e -> {
            try {
                final StringBuffer errors = new StringBuffer();

                if (!verify(errors)) {
                    throw new IOException("Please check your configuration:\n" + errors);
                }

                final InstallationDialog dlg = new InstallationDialog(this);
                dlg.startInstallation(() -> {
                    try {
                        buildApplication(dlg.getTextArea());
                    } catch (final Throwable e1) {
                        JOptionPane.showMessageDialog(Configurator.this, "Failed to install the application!\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        throw new RuntimeException(e1);
                    }
                });
                JOptionPane.showMessageDialog(Configurator.this, "Successfully installed the application!", "Information", JOptionPane.INFORMATION_MESSAGE);
            } catch (final Throwable e1) {
                JOptionPane.showMessageDialog(Configurator.this, "Failed to install the application!\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        final JButton cancelButton = new JButton("Close");
        cancelButton.addActionListener(event -> dispose());
        final GridBagConstraints gbc_cancelButton = new GridBagConstraints();
        gbc_cancelButton.anchor = GridBagConstraints.WEST;
        gbc_cancelButton.gridx = 3;
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


    private String generateConfiguration() throws IOException {
        final StringBuffer errors = new StringBuffer();
        if (!verify(errors)) {
            throw new IOException("Please check your configuration:\n" + errors);
        }

        final StringBuffer config = new StringBuffer();
        config.append("###################################################################\n"
                + "###                  System specific settings                   ###\n"
                + "###################################################################\n"
                + "\n");
        bmsPanel.generateConfiguration(config);
        inverterPanel.generateConfiguration(config);
        pluginsPanel.generateConfiguration(config);
        servicesPanel.generateConfiguration(config);

        return config.toString();
    }


    private void updateConfiguration() throws IOException, URISyntaxException {
        final String config = generateConfiguration();
        // define paths
        final Path installDirectory = Paths.get(generalPanel.getInstallationPath());
        final Path configDirectory = installDirectory.resolve("config");

        // generate the configuration files
        Files.deleteIfExists(configDirectory.resolve("config.properties"));
        Files.write(configDirectory.resolve("config.properties"), config.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        Files.deleteIfExists(configDirectory.resolve("log4j2.xml"));
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("templates/main-log4j2.xml")) {
            final String logConfig = new String(InputStreamUtil.readAllBytes(is));
            Files.write(configDirectory.resolve("log4j2.xml"), logConfig.replace("<Root level=\"info\">", "<Root level=\"" + generalPanel.getLogLevel() + "\">").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
    }


    private void buildApplication(final JTextArea out) throws Throwable {
        final StringBuffer errors = new StringBuffer();
        if (!verify(errors)) {
            throw new IOException("Please check your configuration:\n" + errors);
        }

        // define paths
        final Path installDirectory = Paths.get(generalPanel.getInstallationPath());
        out.append("Installing in: " + installDirectory + "\n");
        final Path configDirectory = installDirectory.resolve("config");
        out.append("Configuration in: " + configDirectory + "\n");
        final Path tempDirectory = installDirectory.resolve("temp");
        out.append("Temp directory is: " + tempDirectory + "\n");
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
        Files.createDirectories(configDirectory);
        Files.createDirectories(tempDirectory);

        // check if previous maven is present
        if (!Files.exists(mavenDirectory)) {
            out.append("Downloading maven...");
            downloadFile(new URL("https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"), mavenZip.toFile());
            out.append("done\n");
            unzip(mavenZip, tempDirectory);
            Files.delete(mavenZip);
            final File mvnFile = mavenDirectory.resolve("bin/mvn").toFile();
            mvnFile.setExecutable(true);
            mvnFile.setReadable(true);
        }

        // download the application source
        out.append("Downloading application...");
        downloadFile(new URL("https://github.com/ai-republic/bms-to-inverter/archive/master.zip"), srcZip.toFile());
        out.append("done\n");
        unzip(srcZip, tempDirectory);

        // copy platform specific CAN library
        String canLibFolder = null;
        switch (generalPanel.getPlatform()) {
            case "AARCH64":
                canLibFolder = "aarch64";
            break;
            case "ARM v5":
                canLibFolder = "armv5";
            break;
            case "ARM v6":
                canLibFolder = "armv6";
            break;
            case "ARM v7":
                canLibFolder = "armv7";
            break;
            case "ARM v7a":
                canLibFolder = "armv7a";
            break;
            case "ARM v7l":
                canLibFolder = "armv7l";
            break;
            case "RISCV v32":
                canLibFolder = "riscv32";
            break;
            case "RISCV v64":
                canLibFolder = "riscv64";
            break;
            case "X86 32bit (UNIX)":
                canLibFolder = "x86_32";
            break;
            case "X86 64bit (UNIX)":
                canLibFolder = "x86_64";
            break;
            case "Windows":
                canLibFolder = "x86_64";
            break;
            default:
            break;
        }

        Files.deleteIfExists(srcDirectory.resolve("protocol-can/src/main/resources/native/libjavacan-core.so"));
        Files.copy(srcDirectory.resolve("protocol-can/src/main/resources/native/" + canLibFolder + "/native/libjavacan-core.so"), srcDirectory.resolve("protocol-can/src/main/resources/native/libjavacan-core.so"));

        if (Files.exists(installDirectory.resolve("lib"))) {
            out.append("Deleting existing lib folder...\n");

            Files.list(installDirectory.resolve("lib")).forEach(file -> {
                try {
                    Files.delete(file);
                } catch (final IOException e) {
                }
            });
        }

        // create a new poms with necessary dependencies only
        generatePOMs(tempDirectory);

        // build the application
        out.append("Building application...\n");
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
            out.append(line + "\n");
        }
        final int exitCode = process.waitFor();

        if (exitCode != 0) {
            out.append("\nFailed to build application!\n");
        } else {
            out.append("\nApplication was successfully built!\n");
        }

        // unzip generated application
        unzip(srcDirectory.resolve("bms-to-inverter-main/target/bms-to-inverter.zip"), installDirectory);
        // copy the configurator app
        Files.deleteIfExists(installDirectory.resolve("lib/configurator.jar"));
        Files.copy(srcDirectory.resolve("configurator/current/configurator.jar"), installDirectory.resolve("lib/configurator.jar"));

        // add the webserver
        Files.deleteIfExists(installDirectory.resolve("lib/webserver-0.0.1-SNAPSHOT.jar"));

        if (servicesPanel.isWebserverEnabled()) {
            Files.copy(srcDirectory.resolve("webserver/target/webserver-0.0.1-SNAPSHOT.jar"), installDirectory.resolve("lib/webserver-0.0.1-SNAPSHOT.jar"));
        }

        // clean up source directory and zip
        Files.deleteIfExists(srcZip);

        if (Files.exists(srcDirectory)) {
            Files.walk(srcDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        // generate the configuration files
        try {
            updateConfiguration();
        } catch (final IOException e) {
            // if something happens here delete the lib directory
            if (Files.exists(installDirectory.resolve("lib"))) {
                Files.walk(installDirectory.resolve("lib"))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
            throw e;
        }

        // generate start scripts
        final Path windowsStart = installDirectory.resolve("start.cmd");
        Files.deleteIfExists(windowsStart);
        Files.write(windowsStart, "start java -DconfigFile=config/config.properties -Dlog4j2.configurationFile=file:config/log4j2.xml -jar lib/bms-to-inverter-main-0.0.1-SNAPSHOT.jar\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        setFilePermissions(windowsStart, true, true, true);

        final Path linuxStart = installDirectory.resolve("start.sh");
        Files.deleteIfExists(linuxStart);
        Files.write(linuxStart, "#!/bin/bash\njava -DconfigFile=config/config.properties -Dlog4j2.configurationFile=file:config/log4j2.xml -jar lib/bms-to-inverter-main-0.0.1-SNAPSHOT.jar\n".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        setFilePermissions(linuxStart, true, true, true);

        final Path windowsConfigStart = installDirectory.resolve("configurator.cmd");
        final Path linuxConfigStart = installDirectory.resolve("configurator.sh");
        Files.deleteIfExists(windowsConfigStart);
        Files.deleteIfExists(linuxConfigStart);
        Files.write(windowsConfigStart, "java -jar lib/configurator.jar".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        Files.write(linuxConfigStart, "#!/bin/bash\njava -jar lib/configurator.jar".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        setFilePermissions(windowsConfigStart, true, true, true);
        setFilePermissions(linuxConfigStart, true, true, true);
    }


    private void generatePOMs(final Path tempDirectory) throws IOException {
        final Path parentPomTemplate = tempDirectory.resolve("bms-to-inverter-main/configurator/src/main/resources/templates/parent-pom.xml");
        final String parentPom = new String(Files.readAllBytes(parentPomTemplate));
        final Path mainPomTempplate = tempDirectory.resolve("bms-to-inverter-main/configurator/src/main/resources/templates/main-pom.xml");
        final String mainPom = new String(Files.readAllBytes(mainPomTempplate));
        final StringBuffer bmsDependencies = new StringBuffer();
        final List<String> modules = new ArrayList<>();
        final Set<String> bmses = new HashSet<>();

        bmsPanel.getBMSConfigList().forEach(bms -> {
            final String bmsName = bms.getDescriptor().getName();

            // check if dependency for the BMS is already added
            if (!bmses.contains(bmsName)) {
                // otherwise create the artifactId from the BMS binding name
                final StringBuffer artifactId = new StringBuffer("bms");

                // check if its the dummy BMS
                if (bmsName.equals("NONE")) {
                    artifactId.append("-dummy");
                } else {
                    final String[] parts = bmsName.split("_");
                    Stream.of(parts).forEach(part -> artifactId.append("-" + part.toLowerCase()));

                    final String protocolModule = getProtocolModule(artifactId.substring(artifactId.lastIndexOf("-") + 1));

                    // add protocol module if not yet added
                    if (protocolModule != null && !modules.contains(protocolModule)) {
                        modules.add(0, protocolModule);
                    }
                }

                // add BMS module
                modules.add("<module>" + artifactId.toString() + "</module>");

                // and add the BMS dependency
                bmses.add(bmsName);
                bmsDependencies.append("     <!--  ####################  " + bmsName + " BMS   ################### -->\r\n"
                        + "        <dependency>\r\n"
                        + "            <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                        + "            <artifactId>" + artifactId + "</artifactId>\r\n"
                        + "            <version>${project.version}</version>\r\n"
                        + "        </dependency>\r\n");

                // check for DALY
                if (bmsName.startsWith("DALY_")) {
                    // add the Daly common library
                    modules.add("<module>bms-daly-common</module>");
                    bmsDependencies.append("     <!--  ####################  " + bmsName + " BMS   ################### -->\r\n"
                            + "        <dependency>\r\n"
                            + "            <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                            + "            <artifactId>bms-daly-common</artifactId>\r\n"
                            + "            <version>${project.version}</version>\r\n"
                            + "        </dependency>\r\n");
                }
            }
        });

        // create the artifactId from the inverter binding name
        final String inverterName = inverterPanel.getInverterType().getName();
        final StringBuffer artifactId = new StringBuffer("inverter");

        if (inverterName.equals("NONE")) {
            artifactId.append("-dummy");
        } else {
            final String[] parts = inverterName.split("_");
            Stream.of(parts).forEach(part -> artifactId.append("-" + part.toLowerCase()));

            final String protocolModule = getProtocolModule(artifactId.substring(artifactId.lastIndexOf("-") + 1));

            // add protocol module if its not yet added
            if (protocolModule != null && !modules.contains(protocolModule)) {
                modules.add(0, protocolModule);
            }
        }

        // and add the inverter dependency
        modules.add("<module>" + artifactId + "</module>");

        final StringBuffer inverterDependencies = new StringBuffer("        <!-- ####################  " + inverterName + " inverter  ################### -->\r\n"
                + "        <dependency>\r\n"
                + "            <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                + "            <artifactId>" + artifactId + "</artifactId>\r\n"
                + "            <version>${project.version}</version>\r\n"
                + "        </dependency>\r\n");

        final StringBuffer serviceDependencies = new StringBuffer();
        // add optional services
        if (servicesPanel.isMQTTProducerEnabled()) {
            // add MQTT client dependencies
            modules.add("<module>service-mqtt-client</module>");

            serviceDependencies.append("        <!-- ####################  MQTT Producer ################### -->\r\n"
                    + "         <dependency>\r\n"
                    + "            <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                    + "            <artifactId>service-mqtt-client</artifactId>\r\n"
                    + "            <version>${project.version}</version>\r\n"
                    + "        </dependency>\r\n"
                    + "");
        }

        if (servicesPanel.isMQTTBrokerEnabled()) {
            // add MQTT client dependencies
            modules.add("<module>service-mqtt-broker</module>");

            serviceDependencies.append("        <!-- ####################  MQTT Broker ################### -->\r\n"
                    + "         <dependency>\r\n"
                    + "            <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                    + "            <artifactId>service-mqtt-broker</artifactId>\r\n"
                    + "            <version>${project.version}</version>\r\n"
                    + "        </dependency>\r\n"
                    + "");
        }

        if (servicesPanel.isEmailEnabled()) {
            // add email service dependencies
            serviceDependencies.append("        <!-- ####################  Email service  ################### -->\r\n"
                    + "         <dependency>\r\n"
                    + "            <groupId>com.ai-republic.email</groupId>\r\n"
                    + "            <artifactId>email-javamail</artifactId>\r\n"
                    + "            <version>1.0.5</version>\r\n"
                    + "        </dependency>\r\n"
                    + "");
        }

        if (servicesPanel.isWebserverEnabled()) {
            // add webserver dependency
            modules.add("<module>webserver</module>");

            serviceDependencies.append("        <!-- ####################  Webserver  ################### -->\r\n"
                    + "         <dependency>\r\n"
                    + "            <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                    + "            <artifactId>webserver</artifactId>\r\n"
                    + "            <version>${project.version}</version>\r\n"
                    + "        </dependency>\r\n"
                    + "");
        }

        // append all dependencies to the parent pom
        final Path parentPomFile = tempDirectory.resolve("bms-to-inverter-main/pom.xml");
        Files.deleteIfExists(parentPomFile);
        final StringBuffer moduleDependencies = new StringBuffer();
        modules.forEach(m -> moduleDependencies.append("\t\t" + m + "\r\n"));
        Files.write(parentPomFile, String.format(parentPom, moduleDependencies.toString()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        // now append all dependencies to the main pom
        final Path mainPomFile = tempDirectory.resolve("bms-to-inverter-main/bms-to-inverter-main/pom.xml");
        Files.deleteIfExists(mainPomFile);
        Files.write(mainPomFile, String.format(mainPom, bmsDependencies.toString(), inverterDependencies.toString(), serviceDependencies.toString()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }


    private String getProtocolModule(final String protocol) {
        switch (protocol) {
            case "can":
                return "<module>protocol-can</module>";
            case "rs485":
                return "<module>protocol-rs485</module>";
            case "modbus":
                return "<module>protocol-modbus</module>";
        }

        return null;
    }


    private void setFilePermissions(final Path path, final boolean read, final boolean write, final boolean execute) {
        final File file = path.toFile();
        file.setWritable(read);
        file.setReadable(write);
        file.setExecutable(execute);
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


    void loadConfiguration(final Path path) {
        // define paths
        final Path installDirectory = path;
        final Path configFilePath = installDirectory.resolve("config/config.properties");
        final Path logFilePath = installDirectory.resolve("config/log4j2.xml");

        // log config.properties and set configuration
        if (Files.exists(configFilePath)) {
            final Properties config = new Properties();
            try {
                config.load(Files.newInputStream(configFilePath));
            } catch (final IOException e) {
                // no existing configuration found
                return;
            }

            try {
                generalPanel.setConfiguration(config);
                bmsPanel.setConfiguration(config);
                inverterPanel.setConfiguration(config);
                pluginsPanel.setConfiguration(config);
                servicesPanel.setConfiguration(config);

                JOptionPane.showMessageDialog(Configurator.this, "Successfully loaded the configuration!", "Information", JOptionPane.INFORMATION_MESSAGE);
                updateConfigButton.setEnabled(true);
            } catch (final Exception e) {
                JOptionPane.showMessageDialog(Configurator.this, "Failed to load the configuration!\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }

        // load log4j2.xml and determine log level
        if (Files.exists(logFilePath)) {
            try {
                final String logXml = new String(Files.readAllBytes(logFilePath));
                final int idx = logXml.indexOf("<Root level=\"") + "<Root level=\"".length();
                final String logLevel = logXml.substring(idx, logXml.indexOf('\"', idx + 1));
                generalPanel.setLogLevel(logLevel);
            } catch (final IOException e) {
                JOptionPane.showMessageDialog(Configurator.this, "Failed to load the log level!\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }

        }
    }


    /**
     * Disables the Update Configuration button.
     */
    public void disableUpdateConfiguration() {
        updateConfigButton.setEnabled(false);
    }

}