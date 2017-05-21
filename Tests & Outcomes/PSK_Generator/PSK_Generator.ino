#include <ESP8266WiFi.h>
#include <time.h>

/*
 * Generate an random password
 */
char *getPSK(int len, int seed) {
    static const char alphanum[] =
        "!@#$%^&*"
        "0123456789"
        "abcdefghijklmnopqrstuvwxyz"
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    //srand(time(NULL));
    randomSeed(seed);
    int alpha_size = strlen(alphanum);
    String str;
    for (int i = 0; i < len; i++) {
        str += alphanum[rand() % alpha_size];
    }
    char *chr = new char[str.length() + 1];
    strcpy(chr, str.c_str());
    return chr;
}

void setup() {
    Serial.begin(9600);
    Serial.println();

    //To perform the comparison, you have to comment one of them to ensure both 
    // of these them running on the same scenario
    //Generate a PSK with a seed of time and print it out
    Serial.printf("PSK (time(NULL)): %s\n", getPSK(32, time(NULL)));
    //Generate a PSK with a seed of ESP.getCycleCount() and print it out
    Serial.printf("PSK (ESP.getCycleCount()): %s\n", getPSK(32, ESP.getCycleCount()));
}

void loop() {
}
