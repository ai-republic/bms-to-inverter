#!/bin/bash

echo Installing prerequisites...
sudo apt install default-jdk
sudo apt install git
sudo apt install maven

echo Creating application folders...
mkdir ~/bms-to-inverter
mkdir ~/bms-to-inverter/src
mkdir ~/bms-to-inverter/app
mkdir ~/bms-to-inverter/config

echo Downloading the sources...
cd ~/bms-to-inverter/src
git clone https://github.com/ai-republic/bms-to-inverter.git

echo Initializing scripts...
cp ~/bms-to-inverter/src/bms-to-inverter/scripts/*.* ~/bms-to-inverter
chmod 777 ~/bms-to-inverter/startcan.sh
chmod 777 ~/bms-to-inverter/cleanrepo.sh
chmod 777 ~/bms-to-inverter/startBmsToInverter.sh
mv ~/bms-to-inverter/pom.xml.mine ~/bms-to-inverter/config
mv ~/bms-to-inverter/config.properties.mine ~/bms-to-inverter/config
mv ~/bms-to-inverter/log4j2.xml.mine ~/bms-to-inverter/config
mv ~/bms-to-inverter/libjavacan-core.so.mine ~/bms-to-inverter/config

echo Done - you are ready to configure your application now!
echo Please configure your system:
echo * ~/bms-to-inverter/config/pom.xml.mine
echo * ~/bms-to-inverter/config/config.properties.mine
echo * ~/bms-to-inverter/config/webserver.properties.mine
echo * ~/bms-to-inverter/config/libjavacan-core.so.mine
echo See documentation at https://github.com/ai-republic/bms-to-inverter/wiki for help