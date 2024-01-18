#!/bin/bash

### start the application
echo Starting the webserver...
cd ~/bms-to-inverter/app
java -jar webserver-0.0.1-SNAPSHOT.jar &