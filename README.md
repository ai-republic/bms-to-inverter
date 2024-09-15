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

_**NOTE:** If you would like me to add a BMS or inverter module just let me know! I would appreciate support to test the BMS and inverter bindings in all variations. Please let me know if you would like to support this project - Testers are very welcome! :)_

----------

## Supported protocols:
* RS485 / UART / RS232
* ModBus
* CAN

----------

## Currently implemented BMS:
* BYD BMS (CAN)
* Daly BMS (CAN / RS485 (& UART / RS232))
* Growatt low voltage (12V/24V/48V) (CAN)
* Growatt high voltage (CAN)
* JBD BMS (RS485 (& UART / RS232))
* JK BMS (CAN / RS485 (& UART / RS232))
* PACE BMS (CAN)
* PylonTech low voltage BMS (CAN / RS485)
* PylonTech high voltage BMS (CAN)
* Samsung BMS (CAN)
* Seplos BMS (CAN)
* SMA BMS (CAN)
* Vertiv BMS (CAN)

## Currently implemented inverters:
* BYD inverters (CAN)
* Fronius inverters
* Growatt low voltage (12V/24V/48V) inverters (CAN)
* Growatt high voltage inverters (CAN)
* PylonTech low voltage inverters (CAN / RS485)
* PylonTech high voltage inverters (CAN)
* Deye inverters (CAN)
* SMA inverters (CAN)
* SolArk inverters (CAN)
* SolPlanet inverters (RS485 / RS232)
* **and any other inverters supporting any of the above listed BMS protocols!**


**NOTE:** **If your BMS or inverter is not in the list it is likely to work with one of these bindings (like Pylon). Just open an issue and we'll see what I can do!**

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

_**NOTE:** There are restrictions using CAN on Windows as SocketCAN library is *NOT* available on Windows OS_

----------

## How to use

See the Wiki page [How to use](https://github.com/ai-republic/bms-to-inverter/wiki/How-to-use) for details on how to install and configure your system using the [Configurator](https://github.com/ai-republic/bms-to-inverter/blob/main/configurator/current/configurator.jar).

**IMPORTANT**: If you use the dummy BMS ( _NONE_ ) binding together with the inverter plugin _SimulatedBatteryPackPlugin_ you **MUST (!!!!!!!!!!) disconnect any _load_ and _PV DC input_**to prevent possible damage!!!

----------

## Notes
If you have questions or need support feel free to contact me or raise an issue or discussion.
If you like to support me testing the application on all different BMSes and inverters please contact me!

_**--> Finally, if you like this project and like to support my work please consider sponsoring this project [`Sponsor`](https://github.com/sponsors/ai-republic) button on the right :)**_

