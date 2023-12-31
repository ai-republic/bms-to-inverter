#!/bin/bash

cd ~/bms-to-inverter
git restore *
git pull
cp ~/pom.xml.mine ~/bms-to-inverter/bms-to-inverter-main/pom.xml
cp ~/config.properties.mine ~/bms-to-inverter/bms-to-inverter-main/src/main/resources/config.properties
cp ~/log4j2.xml.mine ~/bms-to-inverter/bms-to-inverter-main/src/main/resources/log4j2.xml

mvn clean package -DskipTests
rm -R ~/final
mkdir ~/final
unzip ~/bms-to-inverter/bms-to-inverter-main/target/bms-to-inverter-main-0.0.1-SNAPSHOT.zip -d ~/final
cd ~/final/bms-to-inverter-main-0.0.1-SNAPSHOT
java -jar bms-to-inverter-main-0.0.1-SNAPSHOT.jar