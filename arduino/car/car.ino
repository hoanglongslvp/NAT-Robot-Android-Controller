#include "Arduino.h"
#include <LiquidCrystal_I2C.h>

LiquidCrystal_I2C lcd(0x3F, 16, 2);

#define enA 10
#define in1 9
#define in2 8
#define enB 5
#define in3 4
#define in4 3

const int frontTrigger = 7;     // chân trig của HC-SR05
const int frontEcho = 6;     // chân echo của HC-SR05
const int backTrigger = 13;     // chân trig của HC-SR05
const int backEcho = 12;     // chân echo của HC-SR05
int lSpeed = 0;
int rSpeed = 0;
int counter = 0;

int getDistance(int trigger, int echo) {
	long duration, distanceCm;
	digitalWrite(trigger, LOW);
	delayMicroseconds(2);
	digitalWrite(trigger, HIGH);
	delayMicroseconds(5);
	digitalWrite(trigger, LOW);
	duration = pulseIn(echo, HIGH);
	distanceCm = duration * 340 / 20 / 1000;
	return distanceCm;
}

void setup() {
	Serial.begin(9600);

	pinMode( enA, OUTPUT);
	pinMode( in1, OUTPUT);
	pinMode( in2, OUTPUT);
	pinMode( enB, OUTPUT);
	pinMode( in3, OUTPUT);
	pinMode( in4, OUTPUT);

	pinMode(frontTrigger, OUTPUT);   // chân trig sẽ phát tín hiệu
	pinMode(backTrigger, OUTPUT);   // chân trig sẽ phát tín hiệu
	pinMode(frontEcho, INPUT);    // chân echo sẽ nhận tín hiệu
	pinMode(backEcho, INPUT);    // chân echo sẽ nhận tín hiệu

	lcd.begin();
	lcd.backlight();
	lcd.setCursor(0, 0);
	lcd.print("    NAT TEAM    ");
	lcd.setCursor(0, 1);
	lcd.print("AUTOCAR NAT TEAM");
//	delay(1000);
	lcd.clear();
}

void dispDistance() {
	lcd.clear();
	lcd.setCursor(0, 0);
	lcd.print("F: ");
	lcd.print(getDistance(frontTrigger, frontEcho));
	lcd.print(" B: ");
	lcd.print(getDistance(backTrigger, backEcho));
}

void dispSpeed() {
//	lcd.clear();
	lcd.setCursor(0, 1);
	lcd.print("L: ");
	lcd.print(lSpeed);
	lcd.print(" R: ");
	lcd.print(rSpeed);
}

void setRightSpeed(int speed) {
	if (speed < 0) {
		analogWrite( enB, -speed);
		digitalWrite( in3, LOW);
		digitalWrite( in4, HIGH);
	} else {
		analogWrite( enB, speed);
		digitalWrite( in3, HIGH);
		digitalWrite( in4, LOW);
	}
}

void setLeftSpeed(int speed) {
	if (speed < 0) {
		analogWrite( enA, -speed);
		digitalWrite( in1, LOW);
		digitalWrite( in2, HIGH);
	} else {
		analogWrite( enA, speed);
		digitalWrite( in1, HIGH);
		digitalWrite( in2, LOW);
	}
}
void loop() {
	if (counter++ > 360) {
		counter = 0;	}
	if(counter%10==0){
		dispDistance();
		dispSpeed();
	}
	if(counter%2==0){
		Serial.print(String()+"f"+getDistance(frontTrigger, frontEcho)+"\n");
	}
	delay(50);

	//Tinh toan Voltage
//    int sensorValue = analogRead(A0); //read the A0 pin value
//    float voltage = sensorValue * (5.0 / 1023.0);
//    //Serial.println(voltage);
//    lcd.setCursor(0,0);
//    lcd.print("Voltage = ");
//    lcd.print(voltage);
//    lcd.print(" V");
//    int battery; // phan tram pin
//    battery = ((sensorValue * (5.00 / 1023.00)) / 5 * 100);
//    lcd.setCursor(0,1);
//    lcd.print("Battery = ");
//    lcd.print(battery);
//    lcd.print("%");

	while (Serial.available() > 0) {
		String cmd = Serial.readStringUntil(';');
		int speed = cmd.substring(1, cmd.length()).toInt();
		Serial.println("Read command: speed = " + speed);
		if (cmd[0] == 'l') {
			lSpeed = speed;
			setLeftSpeed(speed);
		} else {
			rSpeed = speed;
			setRightSpeed(speed);
		}
		delay(1);
	}

}
