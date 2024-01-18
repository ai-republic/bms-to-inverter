#!/bin/bash


### get a fresh copy
echo Updating the application from GitHub...
cd ~/bms-to-inverter/src
git restore *
git pull


### overwrite the project files with the configured resource files
echo Updating the application with your configuration files...
cp ~/bms-to-inverter/config/pom.xml.mine ~/bms-to-inverter/src/bms-to-inverter/bms-to-inverter-main/pom.xml
cp ~/bms-to-inverter/config/config.properties.mine ~/bms-to-inverter/src/bms-to-inverter/bms-to-inverter-main/src/main/resources/config.properties
cp ~/bms-to-inverter/config/log4j2.xml.mine ~/bms-to-inverter/src/bms-to-inverter/bms-to-inverter-main/src/main/resources/log4j2.xml
cp ~/bms-to-inverter/config/libjavacan-core.so.mine ~/bms-to-inverter/src/bms-to-inverter/protocol-can/src/main/resources/native/libjavacan-core.so
cp ~/bms-to-inverter/config/webserver.properties.mine ~/bms-to-inverter/src/bms-to-inverter/webserver/src/main/resources/application.properties


### build the project
echo Building the application...
mvn clean package -DskipTests


### unpack it in the final folder
echo Unpacking the application to the app folder...
rm -R ~/bms-to-inverter/app
mkdir ~/bms-to-inverter/app
unzip ~/bms-to-inverter/src/bms-to-inverter/bms-to-inverter-main/target/bms-to-inverter-main-0.0.1-SNAPSHOT.zip -d ~/bms-to-inverter/app
cp ~/bms-to-inverter/src/bms-to-inverter/webserver/target/webserver-0.0.1-SNAPSHOT ~/bms-to-inverter/app


### start the application
echo Starting the application...
cd ~/bms-to-inverter/app/bms-to-inverter-main-0.0.1-SNAPSHOT
java -jar bms-to-inverter-main-0.0.1-SNAPSHOT.jar