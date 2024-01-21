# BMS to Solar Inverter communication
(Use, monitor and control any battery brand with any inverter)

This application is reading data from a BMS and sending it to an inverter. This way you have no restriction on what battery brands you can use with your inverter. 
Many inverter manufacturers only allow batteries from certain battery manufacturers and certain models.
This project enables you to read your BMS's data via different protocols - RS485, RS232, UART, ModBus or CAN - and write the battery data to the inverter in a specification that the inverter supports - Pylontech, SMA, Growatt, Deye, SolArk, etc.
You can monitor each of your battery packs cells and view alarm states on the included webserver or hook up via the MQTT broker on your smart home.
Or you can just read out your BMS's data and use the optional MQTT broker or Webserver to monitor your batteries packs and cells wherever you are.

The (reference) project uses a Raspberry Pi 4B with a [Waveshare RS485/CAN](https://www.waveshare.com/rs485-can-hat.htm) hat or [Waveshare 2-Channel CAN FD HAT](https://www.waveshare.com/2-ch-can-fd-hat.htm) module but you can use any CAN or RS485 module for your PI that provides ports like `can0` or `/dev/ttyS0` or similar. It will also work on older/newer PI's such as 3 or 5.
The appplication supports _multiple_ BMS (even mixes from different manufacturers), aggregating them and sending the data to the configurable inverter.

This way _you_ control what gets send to the inverter.


A wide range of BMS and inverters already supported (see below). Any BMS or inverter can be supported in a very short time by just mapping the manufacturers protocol specification in an own implementation of the [`BMS`](https://github.com/ai-republic/bms-to-inverter/blob/main/core-api/src/main/java/com/airepublic/bmstoinverter/core/BMS.java) or [`Inverter`](https://github.com/ai-republic/bms-to-inverter/blob/main/core-api/src/main/java/com/airepublic/bmstoinverter/core/Inverter.java).

**NOTE:** If you would like me to add a BMS or inverter module just let me know! I would appreciate support to test the BMS and inverter bindings in all variations. Please let me know if you would like to support this project - Testers are very welcome! :)

----------

## Supported protocols:
* RS485 / UART / RS232
* ModBus
* CAN

----------

## Currently implemented BMS:
* Daly BMS (RS485(, UART, RS232) or CAN)
* JK BMS (CAN)
* Seplos BMS (CAN)
* PylonTech BMS (CAN)

## Currently implemented inverters:
* SMA Sunny Island (CAN)
* Growatt low voltage (12V/24V/48V) inverters (CAN)
* Deye inverters (CAN)
* SolArk inverters (CAN)
* any inverter speaking the PylonTech (CAN) protocol


----------

## Supported architectures

The following architectures are supported:
* x86_32 
* x86_64
* armv6
* armv7
* armv7a
* armv7l 
* aarch64
* riscv32
* riscv64

**NOTE:** There are restrictions using CAN on Windows as SocketCAN library is *NOT* available on Windows OS.

----------

## How to use

#### *<ins>1. Initialize application and configuration files</ins>*
Download the script [`init.sh`](https://github.com/ai-republic/bms-to-inverter/tree/main/scripts/init.sh) and run the commands:

```
chmod 777 init.sh
init.sh
```
This script will first install any prerequisites like Java JDK, Git and Maven.  Then it will create a folder structure in your home directory called `bms-to-inverter`, download the application source into `~/bms-to-inverter/src`, copy all configuration file templates to `~/bms-to-inverter/config`.

#### *<ins>2. Choose your optional services like MQTT and Email notification</ins>*
In the `~/bms-to-inverter/config/pom.xml.mine` you'll can choose optional services to use. 
See the following section (comment out what you do not need):


```
...

<!-- #################### !!!!!!!!	Choose optional services 	!!!!!!!! ###################### -->

		<!-- optionally add MQTT services -->
		<dependency>
			<groupId>com.ai-republic.bms-to-inverter</groupId>
			<artifactId>service-mqtt-broker</artifactId>
			<version>${project.version}</version>
		</dependency>

		<dependency>
			<groupId>com.ai-republic.bms-to-inverter</groupId>
			<artifactId>service-mqtt-client</artifactId>
			<version>${project.version}</version>
		</dependency>

		
		<!-- optionally add Email service -->
		<dependency>
			<groupId>com.ai-republic.email</groupId>
			<artifactId>email-javamail</artifactId>
			<version>1.0.5</version>
		</dependency>
...

```


#### *<ins>3. Choose your target architecture (!!! only needed if using CAN protocol !!!)</ins>*
In the `~/bms-to-inverter/src/bms-to-inverter/protocol-can/src/main/resources/native` folder you have sub-folders for all the supported architectures. Choose your target architecture and copy the `libjavacan-core.so` file from the appropriate architecture folder and put it in your `~/bms-to-inverter/config` folder and name it `libjavacan-core.so.mine`.

#### *<ins>4. Configuration</ins>*

Check the `~/bms-to-inverter/config/config.properties.mine` in your config folder and configure the number of battery packs (not cells!), port assignments and optional service properties for MQTT and Email.

```
###################################################################
###                  System specific settings                   ###
###################################################################

###################################################################
###                       BMS settings                          ###
###################################################################

####  Configuration per BMS ('x' is incremented per BMS) ####
# bms.x.type - can be (DALY_CAN, DALY_RS485, JK_CAN, PYLON_CAN or SEPLOS_CAN 
# bms.x.portLocator - is the locator/device to use to communicate to the BMS, eg. can0, /dev/ttyUSB0, com3, etc.  
# bms.x.pollIntervall - is the interval to request BMS data (in seconds)
# bms.x.delayAfterNoBytes - is the delay after receiving no data (in ms)
bms.0.type=DALY_CAN
bms.0.portLocator=can0
bms.0.pollInterval=2
bms.0.delayAfterNoBytes=200

#bms.1.type=DALY_CAN
#bms.1.portLocator=can0
#bms.1.pollInterval=2
#bms.1.delayAfterNoBytes=200

#bms.2.type=JK_CAN
#bms.2.portLocator=can1
#bms.2.pollInterval=2
#bms.2.delayAfterNoBytes=200
#
#...


###################################################################
###                    Inverter settings                        ###
###################################################################

# The inverter type can be NONE, DEYE_CAN, GROWATT_CAN, SMA_SI_CAN, SOLARK_CAN
inverter.type=NONE

# The port name/device to use to communicate to the  inverter  
inverter.portLocator=can1

# Interval to send data to the inverter (in seconds) 
inverter.sendInterval=1


###################################################################
###                 Optional services settings                  ###
###################################################################

### MQTT properties
mqtt.locator=tcp://127.0.0.1:61616
mqtt.topic=energystorage

### Email properties
mail.out.debug=true
mail.out.host=smtp.gmail.com
mail.out.port=587
# smtp for TLS, smtps for SSL
mail.out.type=smtp
mail.out.username=
mail.out.password= 
# disable if using TLS
mail.out.sslEnable=false
# disable if using SSL
mail.out.tlsEnable=true
mail.out.defaultEmail=
mail.recipients=
```

If you intend to use the [`webserver`](https://github.com/ai-republic/bms-to-inverter/blob/main/webserver) project to monitor your BMSes you might want to review the `~/bms-to-inverter/config/webserver.properties.mine` to define the server port and make sure that the MQTT properties match those in your `bms-to-inverter/config/config.properties.mine`.

```
# Webserver properties
server.port=8080

# MQTT properties
mqtt.locator=tcp://localhost:61616
mqtt.topic=energystorage
```


#### *<ins>5. Building the application</ins>*

Once your project is configured you can simply build it using the script:

```
~/bms-to-inverter/cleanrepo.sh
```

#### *<ins>6. Start the application</ins>*

Now your application is ready to go! 

If ever you change any configuration in `~/bms-to-inverter/config` you will need to call the `~/bms-to-inverter/cleanrepo.sh` script again.

You can start the application:

```
~/bms-to-inverter/app/startBmsToInverter.sh
```

If you want to use the webserver too, just start it with:

```
~/bms-to-inverter/app/startWebserver.sh
```


----------

## Notes
I will be doing a lot of updates to the documentation and current code and structure but if you have questions or need support feel free to contact me or raise an issue.


*I do not take any responsiblity for any damages that might occur by using this software!*
