#!/bin/bash


### get a fresh copy
echo Updating the application from GitHub...
cd ~/bms-to-inverter
git restore *
git pull


### overwrite the project files with the configured resource files
echo Updating the application with your configuration files...
cp ~/pom.xml.mine ~/bms-to-inverter/bms-to-inverter-main/pom.xml
cp ~/config.properties.mine ~/bms-to-inverter/bms-to-inverter-main/src/main/resources/config.properties
cp ~/log4j2.xml.mine ~/bms-to-inverter/bms-to-inverter-main/src/main/resources/log4j2.xml
cp ~/libjavacan-core.so.mine ~/bms-to-inverter/protocol-can/src/main/resources/native/libjavacan-core.so


### build the project
echo Building the application...
mvn clean package -DskipTests


### unpack it in the final folder
echo Unpacking the application to the final folder...
rm -R ~/final
mkdir ~/final
unzip ~/bms-to-inverter/bms-to-inverter-main/target/bms-to-inverter-main-0.0.1-SNAPSHOT.zip -d ~/final


### start the application
echo Starting the application...
cd ~/final/bms-to-inverter-main-0.0.1-SNAPSHOT
java -jar bms-to-inverter-main-0.0.1-SNAPSHOT.jar