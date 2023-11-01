# BMS to Solar Inverter communication
(Use, monitor and control any battery brand with any inverter)

This application is reading data from a BMS and sending it to an inverter. This way you have no restriction on what battery brands you can use with your inverter. 
Many inverter manufacturers only allow batteries from certain battery manufacturers and certain models.
This project enables you to read your BMS's data (e.g. Daly BMS as reference) via different protocols, e.g. RS485 or CAN, and write the battery data to the inverter in a protocol specification that the inverter accepts.
You can monitor each of your battery packs cells and view alarm states on the included webserver or hook up via the MQTT broker on your smart home.
Or you can just read out your BMS's data and use the optional MQTT broker or Webserver to monitor your batteries packs and cells wherever you are.

The (reference) project uses a Raspberry Pi 4 with a [Waveshare RS485/CAN](https://www.waveshare.com/rs485-can-hat.htm) hat or [Waveshare 2-Channel CAN FD HAT](https://www.waveshare.com/2-ch-can-fd-hat.htm) module but you can use any CAN or RS485 module for your PI that provides ports like `can0` or `ttyS0` or similar.
The appplication supports _multiple_ battery packs, aggregating them and sending the data to the configurable inverter.

This way _you_ control what gets send to the inverter.


Any BMS or inverter can be supported in a very short time by just mapping the manufacturers protocol specification in an own implementation of the [`PortProcessor`](https://github.com/ai-republic/bms-to-inverter/blob/main/core-api/src/main/java/com/airepublic/bmstoinverter/core/PortProcessor.java).

**NOTE:** If you would like me to add a BMS or inverter module just let me know! Growatt inverter tests are still outstanding. I would appreciate support to test the inverter bindings on the different inverters.

----------

## Supported protocols:
* UART / RS485
* ModBus
* CAN

----------

## Currently implemented BMS:
* Daly BMS (RS485 or CAN)
* JK BMS (CAN)*
* Seplos BMS (CAN)*

## Currently implemented inverters:
* SMA Sunny Island (CAN)
* Growatt low voltage (12V/24V/48V) inverters (CAN)*


\* need testing on real hardware - testers welcome!

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
The reference project [`bms-to-inverter-main`](https://github.com/ai-republic/bms-to-inverter/blob/main/bms-to-inverter-main) shows how to communicate with Daly BMS to a Sunny Island inverter. Please make sure you have the right ports/devices configured in [`config.properties`](https://github.com/ai-republic/bms-to-inverter/blob/main/bms-to-inverter-main/src/main/resources/config.properties).

#### *<ins>1. Choose your BMS and inverter with the appropriate protocol</ins>*
In the `pom.xml` file of the [`bms-to-inverter-main`](https://github.com/ai-republic/bms-to-inverter/blob/main/bms-to-inverter-main) project you'll find the dependencies which BMS and which inverter to use. If you're not using Daly BMS to SMA Sunny Island both communicating via CAN protocol you'll have to change the following dependencies according to your BMS, inverter and optional services.

```

		<!--		choose BMS 		-->
		<dependency>
			<groupId>com.ai-republic.bms-to-inverter</groupId>
			<artifactId>bms-daly-can</artifactId>
			<version>${project.version}</version>
		</dependency>
		
<!--		<dependency>-->
<!--			<groupId>com.ai-republic.bms-to-inverter</groupId>-->
<!--			<artifactId>bms-daly-rs485</artifactId>-->
<!--			<version>${project.version}</version>-->
<!--		</dependency>-->



		<!--		choose inverter			-->
		<dependency>
			<groupId>com.ai-republic.bms-to-inverter</groupId>
			<artifactId>inverter-sma-can</artifactId>
			<version>${project.version}</version>
		</dependency>
		
<!--		<dependency>-->
<!--			<groupId>com.ai-republic.bms-to-inverter</groupId>-->
<!--			<artifactId>inverter-growatt-can</artifactId>-->
<!--			<version>${project.version}</version>-->
<!--		</dependency>-->



		<!-- optionally add MQTT and/or email services -->
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
			<version>1.0.0</version>
		</dependency>


```

So if you like to use RS485 protocol to communicate with the Daly BMS you can just change the `bms-daly-can` to `bms-daly-rs485`.

#### *<ins>2. Choose your target architecture (!!! only if using CAN protocol !!!)</ins>*
In the [`native`](https://github.com/ai-republic/bms-to-inverter/blob/main/protocol-can/src/main/resources/native) folder of the  [`protocol-can`](https://github.com/ai-republic/bms-to-inverter/blob/main/protocol-can) project you have sub-folders for all the supported architectures. Choose your target architecture and copy the `libjavacan-core.so` file from the appropriate architecture folder to the [`native`](https://github.com/ai-republic/bms-to-inverter/blob/main/protocol-can/src/main/resources/native) folder.

#### *<ins>3. Configuration</ins>*
Once you have your right dependencies and target architecture defined check the [`config.properties`](https://github.com/ai-republic/bms-to-inverter/blob/main/bms-to-inverter-main/src/main/resources/config.properties) to define the number of battery packs, port assignments and MQTT properties.

```
numBatteryPacks=8

# RS485 properties
RS485.baudrate=9600
RS485.startFlag=165
RS485.frameLength=13

# CAN properties
#bms.portname=com3				# RS485 on Windows for testing
#bms.portname=/dev/ttyS0		# RS485 on Raspberry
bms.portname=can0
inverter.portname=can1		# can1 for Waveshare 2CH-CAN-HAT-FD, otherwise can0 for e.g. Waveshare RS485/CAN hat


#MQTT properties
mqtt.locator=tcp://127.0.0.1:61616
mqtt.topic=energystorage
```

If you intend to use the [`webserver`](https://github.com/ai-republic/bms-to-inverter/blob/main/webserver) project to monitor your BMSes you might want to review the [`application.properties`](https://github.com/ai-republic/bms-to-inverter/blob/main/webserver/src/main/resources) to define the server port and make sure that the MQTT properties match those in your `config.properties`.

```
# Webserver properties
server.port=8080

# MQTT properties
mqtt.locator=tcp://localhost:61616
mqtt.topic=energystorage
```


#### *<ins>4. Building the project</ins>*

Once your project is configured you can simply build it with `mvn clean package` to produce a `zip` file found under the `target` directory.
The `zip` file contains the main jar plus all dependencies in a lib folder.
Copy this to your target machine, e.g. a Raspberry, unpack it and start it with `java -jar bms-to-inverter-main-0.0.1-SNAPSHOT.jar`.

If you're using the [`webserver`](https://github.com/ai-republic/bms-to-inverter/blob/main/webserver) then you'll have to copy the `webserver-0.0.1-SNAPSHOT.jar` found in the `webserver/target` folder to your target machine and start it with `java -jar webserver-0.0.1-SNAPSHOT`.

----------

## Notes
I will be doing a lot of updates to the documentation and current code and structure but if you have questions or need support feel free to contact me or raise an issue.


*I do not take any responsiblity for any damages that might occur by using this software!*
