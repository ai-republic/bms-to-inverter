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
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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

public class Configurator extends JFrame {
    private static final long serialVersionUID = 1L;
    private final GeneralPanel generalPanel;
    private final BMSPanel bmsPanel;
    private final InverterPanel inverterPanel;
    private final ServicesPanel servicesPanel;
    private final String logConfig = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            + "<Configuration>\r\n"
            + "\r\n"
            + "    <Properties>\r\n"
            + "        <Property name=\"name\">BMS-to-Inverter</Property>\r\n"
            + "        <Property name=\"pattern\">%d{yyyy-MM-dd HH:mm:ss.SSS} | %-5.5p | %-10.10t | %-20.20C:%-5.5L | %msg%n</Property>\r\n"
            + "    </Properties>\r\n"
            + "    \r\n"
            + "    <Appenders>\r\n"
            + "        <Console name=\"Console\" target=\"SYSTEM_OUT\">\r\n"
            + "            <PatternLayout pattern=\"${pattern}\"/>\r\n"
            + "        </Console>\r\n"
            + "        <RollingFile name=\"RollingFile\" fileName=\"logs/${name}.log\"\r\n"
            + "                 filePattern=\"logs/$${date:yyyy-MM}/${name}-%d{yyyy-MM-dd}-%i.log.gz\">\r\n"
            + "            <PatternLayout>\r\n"
            + "                <pattern>${pattern}</pattern>\r\n"
            + "            </PatternLayout>\r\n"
            + "            <Policies>\r\n"
            + "                <TimeBasedTriggeringPolicy /><!-- Rotated everyday -->\r\n"
            + "                <SizeBasedTriggeringPolicy size=\"100 MB\"/> <!-- Or every 100 MB -->\r\n"
            + "            </Policies>\r\n"
            + "        </RollingFile>\r\n"
            + "    </Appenders>\r\n"
            + "    \r\n"
            + "    <Loggers>\r\n"
            + "        <Logger name=\"org.jboss.weld\" level=\"error\" additivity=\"false\">\r\n"
            + "            <AppenderRef ref=\"Console\"/>\r\n"
            + "            <AppenderRef ref=\"RollingFile\"/>\r\n"
            + "        </Logger>\r\n"
            + "        <Logger name=\"io.netty\" level=\"error\" additivity=\"false\">\r\n"
            + "            <AppenderRef ref=\"Console\"/>\r\n"
            + "            <AppenderRef ref=\"RollingFile\"/>\r\n"
            + "        </Logger>\r\n"
            + "        <Logger name=\"org.apache.activemq\" level=\"info\" additivity=\"false\">\r\n"
            + "            <AppenderRef ref=\"Console\"/>\r\n"
            + "            <AppenderRef ref=\"RollingFile\"/>\r\n"
            + "        </Logger>\r\n"
            + "        \r\n"
            + "        <Root level=\"debug\"> <!-- We log everything -->\r\n"
            + "            <AppenderRef ref=\"Console\"/> <!-- To console -->\r\n"
            + "            <AppenderRef ref=\"RollingFile\"/> <!-- And to a rotated file -->\r\n"
            + "        </Root>\r\n"
            + "    </Loggers>\r\n"
            + "    \r\n"
            + "</Configuration>";

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

        inverterPanel = new InverterPanel();
        tabbedPane.addTab("Inverter", null, inverterPanel, null);

        final JScrollPane servicesScrollPane = new JScrollPane();
        tabbedPane.addTab("Services", null, servicesScrollPane, null);

        servicesPanel = new ServicesPanel();
        servicesScrollPane.setViewportView(servicesPanel);

        final JPanel buttonPanel = new JPanel();
        final GridBagLayout gbl_buttonPanel = new GridBagLayout();
        gbl_buttonPanel.columnWidths = new int[] { 0, 0, 120, 120 };
        gbl_buttonPanel.rowHeights = new int[] { 0, 50 };
        gbl_buttonPanel.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0 };
        gbl_buttonPanel.rowWeights = new double[] { 0.0, 0.0 };
        buttonPanel.setLayout(gbl_buttonPanel);

        final JButton updateConfigButton = new JButton("Update Configuration");
        final GridBagConstraints gbc_updateConfigButton = new GridBagConstraints();
        gbc_updateConfigButton.insets = new Insets(0, 0, 0, 5);
        gbc_updateConfigButton.gridx = 1;
        gbc_updateConfigButton.gridy = 1;
        buttonPanel.add(updateConfigButton, gbc_updateConfigButton);
        updateConfigButton.addActionListener(e -> {
            try {
                updateConfiguration();
                JOptionPane.showMessageDialog(Configurator.this, "Successfully updated the configuration!", "Information", JOptionPane.INFORMATION_MESSAGE);
            } catch (final IOException e1) {
                JOptionPane.showMessageDialog(Configurator.this, "Failed to update the configuration!\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                final InstallationDialog dlg = new InstallationDialog(this);
                dlg.startInstallation(() -> {
                    try {
                        buildApplication(dlg.getTextArea());
                    } catch (final Throwable e1) {
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
        servicesPanel.generateConfiguration(config);

        return config.toString();
    }


    private void updateConfiguration() throws IOException {
        final String config = generateConfiguration();
        // define paths
        final Path installDirectory = Path.of(generalPanel.getInstallationPath());
        final Path configDirectory = installDirectory.resolve("config");

        // generate the configuration files
        Files.deleteIfExists(configDirectory.resolve("config.properties"));
        Files.write(configDirectory.resolve("config.properties"), config.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        Files.deleteIfExists(configDirectory.resolve("lo4j2.xml"));
        Files.write(configDirectory.resolve("log4j2.xml"), logConfig.toString().replace("<Root level=\"info\">", "<Root level=\"" + generalPanel.getLogLevel() + "\">").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

    }


    private void buildApplication(final JTextArea out) throws Throwable {
        final StringBuffer errors = new StringBuffer();
        if (!verify(errors)) {
            throw new IOException("Please check your configuration:\n" + errors);
        }

        // define paths
        final Path installDirectory = Path.of(generalPanel.getInstallationPath());
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

        // create a new pom with necessary dependencies only
        generatePOM(tempDirectory);

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
        // add the webserver
        Files.deleteIfExists(installDirectory.resolve("lib/webserver-0.0.1-SNAPSHOT.jar"));
        Files.copy(srcDirectory.resolve("webserver/target/webserver-0.0.1-SNAPSHOT.jar"), installDirectory.resolve("lib/webserver-0.0.1-SNAPSHOT.jar"));

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
        Files.writeString(windowsStart, "start java -DconfigFile=config/config.properties -Dlog4j2.configurationFile=file:config/log4j2.xml -jar lib/bms-to-inverter-main-0.0.1-SNAPSHOT.jar\n", StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        setFilePermissions(windowsStart, true, true, true);

        final Path linuxStart = installDirectory.resolve("start.sh");
        Files.deleteIfExists(linuxStart);
        Files.writeString(linuxStart, "#!/bin/bash\njava -DconfigFile=config/config.properties -Dlog4j2.configurationFile=file:config/log4j2.xml -jar lib/bms-to-inverter-main-0.0.1-SNAPSHOT.jar\n", StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        setFilePermissions(linuxStart, true, true, true);

        if (servicesPanel.isWebserverEnabled()) {
            final Path windowsStartWebserver = installDirectory.resolve("startWebserver.cmd");
            Files.deleteIfExists(windowsStartWebserver);
            Files.writeString(windowsStartWebserver, "start java -jar lib/webserver-0.0.1-SNAPSHOT.jar --spring.config.location=file:config/config.properties\n", StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            setFilePermissions(windowsStartWebserver, true, true, true);

            final Path linuxStartWebServer = installDirectory.resolve("startWebserver.sh");
            Files.deleteIfExists(linuxStartWebServer);
            Files.writeString(linuxStartWebServer, "#!/bin/bash\njava -jar lib/webserver-0.0.1-SNAPSHOT.jar --spring.config.location=file:config/config.properties\n", StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            setFilePermissions(linuxStartWebServer, true, true, true);

        }

        final Path windowsConfigStart = installDirectory.resolve("configurator.cmd");
        final Path linuxConfigStart = installDirectory.resolve("configurator.sh");
        Files.deleteIfExists(windowsConfigStart);
        Files.deleteIfExists(linuxConfigStart);
        Files.writeString(windowsConfigStart, "java -jar lib/configurator-0.0.1-SNAPSHOT.jar", StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        Files.writeString(linuxConfigStart, "#!/bin/bash\njava -jar lib/configurator-0.0.1-SNAPSHOT.jar", StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        setFilePermissions(windowsConfigStart, true, true, true);
        setFilePermissions(linuxConfigStart, true, true, true);
    }


    private void generatePOM(final Path tempDirectory) throws IOException {
        final StringBuffer pom = new StringBuffer("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n"
                + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
                + "    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
                + "    <modelVersion>4.0.0</modelVersion>\r\n"
                + "\r\n"
                + "    <artifactId>bms-to-inverter-main</artifactId>\r\n"
                + "\r\n"
                + "    <parent>\r\n"
                + "        <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                + "        <artifactId>bms-to-inverter-parent</artifactId>\r\n"
                + "        <version>0.0.1-SNAPSHOT</version>\r\n"
                + "    </parent>\r\n"
                + "\r\n"
                + "    <name>${project.artifactId}-${project.version}</name>\r\n"
                + "    <description>Application to communicate between a BMS and inverter</description>\r\n"
                + "\r\n"
                + "    <properties>\r\n"
                + "        <encoding>UTF-8</encoding>\r\n"
                + "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\r\n"
                + "    </properties>\r\n"
                + "\r\n"
                + "    <build>\r\n"
                + "        <plugins>\r\n"
                + "            <plugin>\r\n"
                + "                <artifactId>maven-jar-plugin</artifactId>\r\n"
                + "                <version>3.3.0</version>\r\n"
                + "                <configuration>\r\n"
                + "                    <archive>\r\n"
                + "                        <manifest>\r\n"
                + "                            <addClasspath>true</addClasspath>\r\n"
                + "                            <mainClass>\r\n"
                + "                                com.airepublic.bmstoinverter.BmsToInverter</mainClass>\r\n"
                + "                        </manifest>\r\n"
                + "                    </archive>\r\n"
                + "                </configuration>\r\n"
                + "            </plugin>\r\n"
                + "\r\n"
                + "            <plugin>\r\n"
                + "                <groupId>org.apache.maven.plugins</groupId>\r\n"
                + "                <artifactId>maven-dependency-plugin</artifactId>\r\n"
                + "                <version>3.6.0</version>\r\n"
                + "                <executions>\r\n"
                + "                    <execution>\r\n"
                + "                        <id>copy-dependencies</id>\r\n"
                + "                        <phase>package</phase>\r\n"
                + "                        <goals>\r\n"
                + "                            <goal>copy-dependencies</goal>\r\n"
                + "                        </goals>\r\n"
                + "                        <configuration>\r\n"
                + "                            <outputDirectory>${project.build.directory}/lib</outputDirectory>\r\n"
                + "                            <overWriteReleases>false</overWriteReleases>\r\n"
                + "                            <overWriteSnapshots>false</overWriteSnapshots>\r\n"
                + "                            <overWriteIfNewer>true</overWriteIfNewer>\r\n"
                + "                        </configuration>\r\n"
                + "                    </execution>\r\n"
                + "                </executions>\r\n"
                + "            </plugin>\r\n"
                + "\r\n"
                + "\r\n"
                + "            <plugin>\r\n"
                + "                <groupId>org.apache.maven.plugins</groupId>\r\n"
                + "                <artifactId>maven-assembly-plugin</artifactId>\r\n"
                + "                <configuration>\r\n"
                + "                    <finalName>bms-to-inverter</finalName>\r\n"
                + "                    <appendAssemblyId>false</appendAssemblyId>\r\n"
                + "                </configuration>\r\n"
                + "                <executions>\r\n"
                + "                    <execution>\r\n"
                + "                        <id>create-distribution</id>\r\n"
                + "                        <phase>package</phase>\r\n"
                + "                        <goals>\r\n"
                + "                            <goal>single</goal>\r\n"
                + "                        </goals>\r\n"
                + "                        <configuration>\r\n"
                + "                            <descriptors>\r\n"
                + "                                <descriptor>assembly/zip.xml</descriptor>\r\n"
                + "                            </descriptors>\r\n"
                + "                        </configuration>\r\n"
                + "                    </execution>\r\n"
                + "                </executions>\r\n"
                + "            </plugin>\r\n"
                + "        </plugins>\r\n"
                + "    </build>\r\n"
                + "\r\n"
                + "\r\n"
                + "    <dependencies>\r\n"
                + "        <dependency>\r\n"
                + "            <groupId>org.jboss.weld.se</groupId>\r\n"
                + "            <artifactId>weld-se-shaded</artifactId>\r\n"
                + "            <version>5.1.1.Final</version>\r\n"
                + "        </dependency>\r\n"
                + "\r\n"
                + "        <dependency>\r\n"
                + "            <groupId>com.ai-republic.email</groupId>\r\n"
                + "            <artifactId>email-api</artifactId>\r\n"
                + "            <version>1.0.5</version>\r\n"
                + "        </dependency>\r\n"
                + "\r\n"
                + "        <dependency>\r\n"
                + "            <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                + "            <artifactId>configurator</artifactId>\r\n"
                + "            <version>${project.version}</version>\r\n"
                + "        </dependency>\r\n"
                + "");

        final Map<String, String> dependencies = new HashMap<>();
        bmsPanel.getBMSConfigList().forEach(bms -> {
            final String bmsName = bms.getDescriptor().getName();

            // check if dependency for the BMS is already added
            if (!dependencies.containsKey(bmsName)) {
                // otherwise create the artifactId from the BMS binding name
                final String[] parts = bmsName.split("_");
                final StringBuffer artifactId = new StringBuffer("bms-");
                Stream.of(parts).forEach(part -> artifactId.append("-" + part.toLowerCase()));

                // and add the dependency
                dependencies.put(bmsName, "<!--  ####################  " + bmsName + " BMS   ################### -->\r\n"
                        + "        <dependency>\r\n"
                        + "            <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                        + "            <artifactId>" + artifactId + "</artifactId>\r\n"
                        + "            <version>${project.version}</version>\r\n"
                        + "        </dependency>\r\n");
            }
        });

        // now append all BMS dependencies to the pom
        dependencies.values().forEach(pom::append);

        // create the artifactId from the inverter binding name
        final String inverterName = inverterPanel.getInverterType().getName();
        final StringBuffer artifactId = new StringBuffer("inverter-");

        if (inverterName.equals("NONE")) {
            artifactId.append("dummy");
        } else {
            final String[] parts = inverterName.split("_");
            Stream.of(parts).forEach(part -> artifactId.append("-" + part.toLowerCase()));
        }

        // and add the inverter dependency
        pom.append("<!-- ####################  " + inverterName + " inverter  ################### -->\r\n"
                + "        <dependency>\r\n"
                + "            <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                + "            <artifactId>" + artifactId + "</artifactId>\r\n"
                + "            <version>${project.version}</version>\r\n"
                + "        </dependency>\r\n");

        // add optional services
        if (servicesPanel.isMQTTEnabled()) {
            // add webserver and broker dependencies
            pom.append("        <!-- ####################  MQTT Producer ################### -->\r\n"
                    + "         <dependency>\r\n"
                    + "            <groupId>ccom.ai-republic.email</groupId>\r\n"
                    + "            <artifactId>service-mqtt-client</artifactId>\r\n"
                    + "            <version>1.0.5</version>\r\n"
                    + "        </dependency>\r\n"
                    + "");
        }

        if (servicesPanel.isWebserverEnabled()) {
            // add webserver and broker dependencies
            pom.append("        <!-- ####################  MQTT Broker  ################### -->\r\n"
                    + "         <dependency>\r\n"
                    + "            <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                    + "            <artifactId>service-mqtt-broker</artifactId>\r\n"
                    + "            <version>${project.version}</version>\r\n"
                    + "        </dependency>\r\n"
                    + "");
        }

        if (servicesPanel.isWebserverEnabled()) {
            // add webserver and broker dependencies
            pom.append("        <!-- ####################  MQTT Broker  ################### -->\r\n"
                    + "         <dependency>\r\n"
                    + "            <groupId>com.ai-republic.bms-to-inverter</groupId>\r\n"
                    + "            <artifactId>service-mqtt-broker</artifactId>\r\n"
                    + "            <version>${project.version}</version>\r\n"
                    + "        </dependency>\r\n"
                    + "");
        }

        pom.append("\r\n"
                + "    </dependencies>\r\n"
                + "</project>");

        final Path pomFile = tempDirectory.resolve("pom.xml");
        Files.deleteIfExists(pomFile);
        Files.writeString(pomFile, pom.toString(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
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

        final Properties config = new Properties();
        try {
            config.load(Files.newInputStream(configFilePath));
        } catch (final IOException e) {
            // no existing configuration found
            return;
        }

        try {
            final String logXml = Files.readString(logFilePath);
            final int idx = logXml.indexOf("<root level=\"") + "<root level=\"".length();
            final String logLevel = logXml.substring(idx, logXml.indexOf('\"', idx + 1));
            generalPanel.setLogLevel(logLevel);
            generalPanel.setConfiguration(config);
            bmsPanel.setConfiguration(config);
            inverterPanel.setConfiguration(config);
            servicesPanel.setConfiguration(config);

            JOptionPane.showMessageDialog(Configurator.this, "Successfully loaded the configuration!", "Information", JOptionPane.INFORMATION_MESSAGE);
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(Configurator.this, "Failed to load the configuration!\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

}
