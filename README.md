# MyIoT

What is this project:
This project focuses on the configuration part of the IoT devices, which aims to provide an secured and convenient method to setup devices.


IoT device: NodeMCU 
Contorller (Mobile application): Android

Developemnt Environments:
devce: Arduino
controller: Android Studio

Dependencies - device:
1. Arduino core for ESP8266 WiFi chip: https://github.com/esp8266/Arduino

Dependencies - controller:
1. zxing: https://github.com/zxing/zxing

Set up device:
1. Open ESP8266.ino with Arduino and burn it to a NodeMCU
2. the deivce will boot up and then get the SSID and PSK from the monitor in the Arduino
3. Paste the SSID and PSK (only the SSID and PSK, no additional info and seperate them with a line seperator) into any online QR code generator
4. Generator an QR code which will be used in following step

Set up controller: Install the Android mobile application


Experiments tips:
1. Open the app and tap 'Add Device' to add a new device
2. Swipe right and scan the QR code to acquire the SSID and PSK of the device
3. Swipe right to connect to the device
4. Swipe right to configure the device.
5. Once the device blinks twice, it means that it has connected to the Home Wifi you ask to connect; Once the led turns off, it means that it has connected to the Home Wifi, and you are ready to press the Back key to navigate back to the home page
6. Tap 'Refresh' to scan the conneted devices (note that you have to connect to same Home Wifi as the device, otherwise it will not work)
7. Long press the device in the home page to further configure the device


More details will be added later if I have time. Enjoy!