# BMS to Solar Inverter communication
(Use, monitor and control any battery brand with any inverter)

This application is reading data from a BMS and sending it to an inverter. This way you have no restriction on what battery brands you can use with your inverter. 
Many inverter manufacturers only allow batteries from certain battery manufacturers and certain models.
This project enables you to read your BMS's data via different protocols - RS485, RS232, UART, ModBus or CAN - and write the battery data to the inverter in a specification that the inverter supports - Pylontech, SMA, Growatt, Deye, SolArk, etc.
The appplication supports _multiple_ BMS (even mixes from different manufacturers), aggregating them and sending the data to the configurable inverter.
You can monitor each of your battery packs cells and view alarm states on the included webserver or hook up via the MQTT broker on your smart home.
It event let's you *manipulate* or *simulate* BMS data that get's sent to the inverter! Please see read plugin information in the [Wiki](https://github.com/ai-republic/bms-to-inverter/wiki/How-to-use).
This way _you_ control what get's send to the inverter!

The (reference) project uses a Raspberry Pi 4B with a [Waveshare RS485/CAN](https://www.waveshare.com/rs485-can-hat.htm) hat or [Waveshare 2-Channel CAN FD HAT](https://www.waveshare.com/2-ch-can-fd-hat.htm) module but you can use any CAN or RS485 module for your PI that provides ports like `can0` or `/dev/ttyS0` or similar. It will also work on older/newer PI's such as RPi 1 or RPi 5.

A wide range of BMS and inverters already supported, see [Supported-BMSes-and-Inverters](https://github.com/ai-republic/bms-to-inverter/wiki/Supported-BMSes-and-Inverters) in the Wiki.

**NOTE:** **If your BMS or inverter is not in the list it is likely to work with one of these bindings (like Pylon). Just open an issue and we'll see what I can do!**
**NOTE:** I would appreciate support to test the BMS and inverter bindings in all variations. Please let me know if you would like to support this project - Testers are very welcome! :)_
**NOTE** USB CAN adapters that do not create a proper CAN device but only a ttyUSB device have found to be problematic. So please choose the right hardware.

----------

## Supported protocols:
* RS485 / UART / RS232
* ModBus
* CAN

_**NOTE:** There are restrictions using CAN on Windows as SocketCAN library is *NOT* available on Windows OS_

----------

## Requirements

This project explicitly supports Java 8 because its the latest version with 32-bit JDK 8 support that many microcontrollers support.
If you're using a Raspberry PI 3B, 4B or above I recommend to use a 64-bit Java JDK and 64-bit operating system like Raspian OS or Ubuntu.
The application has also been tested on Pi 1 - Pi5. Any microcontroller that can run a JDK 8+ can be used.
For detailed requirements please refer to the [How-to-Use](https://github.com/ai-republic/bms-to-inverter/wiki/How-to-use) in the Wiki.

----------

## How to use

See the Wiki page [How to use](https://github.com/ai-republic/bms-to-inverter/wiki/How-to-use) for details on how to install and configure your system using the [Configurator](https://github.com/ai-republic/bms-to-inverter/blob/main/configurator/current/configurator.jar).

**IMPORTANT**: If you use the dummy BMS (_NONE_) binding together with the inverter plugin _SimulatedBatteryPackPlugin_ you **MUST (!!!!!!!!!!) disconnect any _load_ and _PV DC input_** to prevent possible damage as the inverter will try charging/discharging!!!

----------

## Other Notes
**DISCLAIMER** I do not take _any_ responsibility for _any_ kind of damage or injury that might be caused by using this software. Use at your own risk.
If you have questions or need support feel free to contact me or raise an issue or discussion.
If you like to support me testing the application on all different BMSes and inverters please contact me!

## _**=====>>>>   Finally, if you like this project and like to support my work please consider sponsoring this project [`Sponsor`](https://github.com/sponsors/ai-republic) button on the right ❤️   <<<<=====**_

