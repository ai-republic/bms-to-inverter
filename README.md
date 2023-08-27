# BMS to Solar Inverter communication

Here's some nice way to read your BMS's data (e.g. Daly as reference) via different protocols and read/write the battery data to the inverter.
The (reference) project uses a Raspberry Pi 4 with a Waveshare RS485/CAN hat or USB-CAN-B module getting the data from _multiple_ battery packs, aggregating them and sending the data to the SMA Sunny Island inverter.
Any BMS or inverter can be supported in a very short time by just mapping the manufacturers protocol specification.

Currently implemented BMS:
* Daly BMS (RS485 or CAN)

Currently implemented inverters:
* SMA Sunny Island

These lists can be easily be extended by implementing an own [PortProcessor](https://github.com/ai-republic/bms-to-inverter/blob/main/bms-to-inverter-core/src/main/java/com/airepublic/bmstoinverter/PortProcessor.java) which handles the protocol messages.

## Supported protocols:
* UART / RS485
* CAN


### Supported architectures

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

There are restrictions using CAN on Windows as SocketCAN library is *NOT* available on Windows OS.


### How to use

*I do not take any responsiblity for any damages that might occur by using this software!*