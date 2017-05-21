#include <ESP8266WiFi.h>

void setup() {
    Serial.begin(9600);
    Serial.println();
    
    //Print the MAC address of this device
    Serial.printf("MAC: %s\n", WiFi.softAPmacAddress().c_str());
    //Remove unnecessary parts of the MAC address and generate a SSID 
    String macStr = WiFi.softAPmacAddress();
    macStr.replace(":", "");
    String ssidStr = "ESP_";
    ssidStr += macStr.substring(6, 12);
    //Print the SSID generated
    Serial.printf("SSID: %s\n", ssidStr.c_str());
}

void loop() {
}
