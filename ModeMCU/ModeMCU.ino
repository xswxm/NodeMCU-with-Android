#include <ESP8266WiFi.h>
#include <WiFiClient.h> 
#include <ESP8266WebServer.h>
#include <EEPROM.h>

#define LED 2
#define LIGHTBULB 16
#define USER_RESET 0
#define FACTORY_RESET 4

ESP8266WebServer server(80);

bool ap_mode = true;    //indicate if ESP needs AP mode
bool sta_mode = false;    //indicate if ESP needs STA mode

/*
 * Blink the LED
 * pin: the pin the LED used
 * num: times to blink
 */
void blinkLED(int pin, int num, int interval) {
    for (int i = 0; i < num; i++) {
        digitalWrite(pin, 0);
        delay(interval);
        digitalWrite(pin, 1);
        delay(interval);
    }
}

/*
 * Handel users' configurations and write them to ESP
 */
void configure() {
    //Turn on the LED: notify users the message is received
    digitalWrite(LED, 0);
    Serial.println();
    Serial.println("--------Set configuration start--------");
    //Check if the message receivced match our requirements
    if (server.hasArg("ssid") && server.hasArg("psk") && server.hasArg("title")) {
        //Write configuration
        clearEEPROM(128, 256);
        writeEEPROM(128, 160, server.arg("ssid").c_str());
        writeEEPROM(160, 192, server.arg("psk").c_str());
        writeEEPROM(224, 256, server.arg("title").c_str());
        if (server.hasArg("uname")) {
            writeEEPROM(192, 224, server.arg("uname").c_str());
        }
        sta_mode = true;
        connetHomeWiFi();
    }
    else if (server.hasArg("title")){
        //Write configuration
        clearEEPROM(224, 256);
        writeEEPROM(224, 256, server.arg("title").c_str());
    }
    server.send(200, "text/plain", readEEPROM(224, 256));
    Serial.println("--------Set configuration end----------");
    //Blink the LED twice: notify users the configuration is satisfied
    blinkLED(LED, 2, 100);
}

/*
 * Get Device's title and value
 */
void getDevice() {
    server.send(200, "text/plain", String(readEEPROM(224, 256)) + char(0) + String(digitalRead(LIGHTBULB)));
    //Blink the LED once to notify users it received a request
    blinkLED(LED, 1, 100);
}

/*
 * Set the status of the device
 */
void setDevice() {
    if (server.hasArg("LightBulb")) {
        String val = server.arg("LightBulb");
        if (val == "on") {
            digitalWrite(LIGHTBULB, 0);
            server.send(200, "text/plain", "on");
        }
        else {
            digitalWrite(LIGHTBULB, 1);
            server.send(200, "text/plain", "off");
        }
        Serial.print("LIGHTBULB is ");
        Serial.println(val);
    }
    //Blink the LED once to notify users it received a request
    blinkLED(LED, 1, 100);
}


/*
 * Clear EEPROM, which store all settings
 */
void clearEEPROM(int st, int ed) {
    for (int i = st; i < ed; i++) {
        EEPROM.write(i, 0);
    }
    EEPROM.commit();
    //Serial.println("EEPROM Cleared!");
    Serial.printf("clearEEPROM (%d, %d).\n", st, ed);
}

/*
 * Read content from certain part of the EEPROM
 */
char *readEEPROM(int st, int ed) {
    String str;
    int block;
    Serial.print("readEEPROM: ");
    for (int i = st; i < ed; i++) {
        block = EEPROM.read(i);
        Serial.print(char(block));
        if (block == 0) {
            break;
        }
        str += char(block);
    }
    Serial.println();
    char *chr = new char[str.length() + 1];
    strcpy(chr, str.c_str());
    return chr;
}

/*
 * Write content to certain part of the EEPROM
 */
void writeEEPROM(int st, int ed, const char *chr) {
    for (int i = 0; i < strlen(chr); i++) {
      EEPROM.write(i + st, chr[i]);
    }
    EEPROM.commit();
    Serial.printf("writeEEPROM (%d, %d): %s\n", st, ed, chr);
}

/*
 * Generate an unique SSID based on MAC Address for ESP's Access Point
 */
char *getSSID() {
    String ssidStr = "ESP_";
    Serial.printf("MAC: %s\n", WiFi.softAPmacAddress().c_str());
    String macStr = WiFi.softAPmacAddress();
    macStr.replace(":", "");
    ssidStr += macStr.substring(6, 12);
    char *chr = new char[ssidStr.length() + 1];
    strcpy(chr, ssidStr.c_str());
    return chr;
}

/*
 * Generate an random password
 */
char *getPSK(int len) {
    static const char alphanum[] =
        "!@#$%^&*"
        "0123456789"
        "abcdefghijklmnopqrstuvwxyz"
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    //srand(time(NULL));
    randomSeed(ESP.getCycleCount());
    int alpha_size = strlen(alphanum);
    String str;
    for (int i = 0; i < len; i++) {
        str += alphanum[rand() % alpha_size];
    }
    char *chr = new char[str.length() + 1];
    strcpy(chr, str.c_str());
    return chr;
}

/*
 * Initialize ESP
 * 1. set buttons and LED pins;
 * 2. generate a SSID and Password for AP if it have not been gererated
 * 3. turn the led off.
 */
void initDevice() {
    delay(1000);
    pinMode(LED, OUTPUT);
    pinMode(LIGHTBULB, OUTPUT);
    //pinMode(LIGHTBULB, INPUT_PULLUP);
    pinMode(USER_RESET, INPUT_PULLUP);  //for reseting users' configurations
    pinMode(FACTORY_RESET, INPUT_PULLUP);  //for reseting all configurations, including the password
    Serial.begin(9600);
    EEPROM.begin(512);

    //Turn on the LED and turn off the LightBowl
    digitalWrite(LED, 0);
    digitalWrite(LIGHTBULB, 1);

    //Generate ssid and random password for the first boot
    if (strlen(readEEPROM(0, 32)) == 0) {
        writeEEPROM(0, 32, getSSID());
        writeEEPROM(32, 96, getPSK(32));
    }
    
    if (strlen(readEEPROM(128, 160)) == 0) {
        sta_mode = false;
    }
    else {
        sta_mode = true;
    }
}

/*
 * Create an AP and an HTTPServer for configuration
 */
void createHTTPServer() {
    Serial.println("--------Create HTTPServer start--------");
    Serial.println("Configuring access point...");
    WiFi.softAP(readEEPROM(0, 32), readEEPROM(32, 96));
    Serial.println("Access Point is created.");
    IPAddress myIP = WiFi.softAPIP();
    Serial.print("IP address: ");
    Serial.println(myIP);
    server.on("/configure", configure);
    server.on("/getDevice", getDevice);
    server.on("/setDevice", setDevice);
    server.begin();
    Serial.println("HTTP server is created.");
    Serial.println("--------Create HTTPServer end----------");
}

/*
 * Start HTTPServer
 * ap_mode should be set to true because ESP is running an AP
 */
void enableAP() {
    ap_mode = true;
    Serial.println("Access Point is enabled.");
}

/*
 * Stop HTTPServer
 * ap_mode should be set to false because we do not need AP anymore
 */
void disableAP() {
    ap_mode = false;
    Serial.println("Access Point is disabled.");
}

/*
 * Connet to Home WiFi
 */
void connetHomeWiFi() {
    WiFi.disconnect();
    Serial.println("Connecting to HomeWiFi...");
    WiFi.begin(readEEPROM(128, 160), readEEPROM(160, 192));
}

void setup() {
    initDevice();
    createHTTPServer();

    if (ap_mode) {
        enableAP();
    }
    if (sta_mode) {
        connetHomeWiFi();
    }
}

/*
 * Check if users click any buttons
 */
void clickCheck() {
    // Erase all user settings, disable STA mode and enable AP mode
    if (digitalRead(USER_RESET) == 0) {
        clearEEPROM(128, 256);
        Serial.println();
        Serial.println("User settings have been cleaned.");
        WiFi.mode(WIFI_AP);
        sta_mode = false;
        enableAP();
        digitalWrite(LED, 1);
    }
    // Be care to press this button, you probably have to recreate an QR code
    // since the password will be changed after pressing this button
    if (digitalRead(FACTORY_RESET) == 0) {
        clearEEPROM(0, 512);
        Serial.println();
        Serial.println("All settings have been cleaned, rebooting device...");
        ESP.reset();    //reboot ESP
    }
}

void loop() {
    clickCheck();
    delay(100);
    server.handleClient();
    if (ap_mode && sta_mode) {
        if (WiFi.status() != WL_CONNECTED) {
            Serial.print(".");
        }
        else {
            // If ESP connected to the Home Wifi
            // 1. disaply its IP address
            // 2. set esp to work only on STA mode
            // 3. disable AP mode
            // 4. turn on the LED: Home Wifi is connected
            Serial.println();
            Serial.println("Home WiFi connected");
            Serial.print("IP address: ");
            Serial.println(WiFi.localIP());
            WiFi.mode(WIFI_STA);
            disableAP();
            //Blink the LED 3 times to notify users it has connected to the Home WiFi
            blinkLED(LED, 3, 100);
            //Start SocketIO, connect server
        }
    }
    else if (ap_mode) {
        server.handleClient();
    }
    else if (sta_mode) {
        // If ESP somehow dropped the Home Wifi connection
        // 1. enale AP mode;
        // 2. turn off the LED: Home Wifi is disconnected
        if (WiFi.status() != WL_CONNECTED) {
            Serial.println("Home WiFi dropped, reconnecting...");
            WiFi.mode(WIFI_AP_STA);
            enableAP();
            //Turn on the LED to notify users the connection is dropped
            digitalWrite(LED, 0);
            //end SocketIO server, disconnect server
        }
    }
}
