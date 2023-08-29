# BMS to Solar Inverter communication

Here's some nice way to read your BMS's data (e.g. Daly BMS as reference) via different protocols and read/write the battery data to the inverter.
The (reference) project uses a Raspberry Pi 4 with a Waveshare RS485/CAN hat or USB-CAN-B module getting the data from _multiple_ battery packs, aggregating them and sending the data to the SMA Sunny Island inverter.
Any BMS or inverter can be supported in a very short time by just mapping the manufacturers protocol specification in an own implementation of the [`PortProcessor`](https://github.com/ai-republic/bms-to-inverter/blob/main/bms-to-inverter-core/src/main/java/com/airepublic/bmstoinverter/PortProcessor.java).

Currently implemented BMS:
* Daly BMS (RS485 or CAN)

Currently implemented inverters:
* SMA Sunny Island (CAN)

----------

## Supported protocols:
* UART / RS485
* CAN

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
If you want to communicate with Daly BMS to a Sunny Island you can use the [`bms-to-inverter-main`](https://github.com/ai-republic/bms-to-inverter/blob/main/bms-to-inverter-main) project and build it. Please make sure you have the right ports/devices configured in [`config.properties`](https://github.com/ai-republic/bms-to-inverter/blob/main/bms-to-inverter-main/src/main/resources/config.properties).

Otherwise you'll have to do the following steps:

1. Create your own mapping of BMS and inverter by editing the POM file of the [`bms-to-inverter-main`](https://github.com/ai-republic/bms-to-inverter/blob/main/bms-to-inverter-main) and choose the corresponding BMS and inverter module. 
2. If you're using CAN choose the right [`libjavacan-core.so`](https://github.com/ai-republic/bms-to-inverter/blob/main/bms-to-inverter-can-javacan/src/main/resources/native) for your target architecture
3. Then rebuild it with `mvn clean package` to produce the fat jar found under the `target` directory.
4. Start the jar with `java -jar bms-to-inverter-main-0.0.1-SNAPSHOT-jar-with-dependencies.jar`.

----------

## Notes
I will be doing a lot of updates to the documentation and current code and structure but if you have questions or need support feel free to contact me or raise an issue.


*I do not take any responsiblity for any damages that might occur by using this software!*