#include "Arduino.h"
#include <LiquidCrystal_I2C.h>
#include <IRremote.h>
#include "IRKeyCode.h"

const int IR_RECV_PIN = A1;
const int enA = 10;
const int in1 = 9;
const int in2 = 8;

const int enB = 5;
const int in3 = 4;
const int in4 = 11; //from 3

const int left_sensor = 2;
const int right_sensor = 3;
int lTurn = 0;
int rTurn = 0;

const int frontEcho = 6;     // chân echo của HC-SR05
const int frontTrigger = 7;     // chân trig của HC-SR05
const int backTrigger = 13;     // chân trig của HC-SR05
const int backEcho = 12;     // chân echo của HC-SR05

int lSpeed = 0;
int rSpeed = 0;
int counter = 0;

#define TURN90 19
#define TURN_SPEED 160

LiquidCrystal_I2C lcd(0x3F, 16, 2);
IRrecv irrecv(IR_RECV_PIN);
decode_results results;

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
	irrecv.enableIRIn(); // Start the receiver
	pinMode(enA, OUTPUT);
	pinMode(in1, OUTPUT);
	pinMode(in2, OUTPUT);

	pinMode(enB, OUTPUT);
	pinMode(in3, OUTPUT);
	pinMode(in4, OUTPUT);

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

void dispBattery() {

//	Tinh toan Voltage
	int sensorValue = analogRead(A0); //read the A0 pin value
	float voltage = sensorValue * (5.0 / 1023.0);
	//Serial.println(voltage);
	lcd.setCursor(0, 0);
	lcd.print("Voltage = ");
	lcd.print(voltage);
	lcd.print(" V");
	int battery; // phan tram pin
	battery = ((sensorValue * (5.00 / 1023.00)) / 5 * 100);
	lcd.setCursor(0, 1);
	lcd.print("Battery = ");
	lcd.print(battery);
	lcd.print("%");
}

void dispSpeed() {
//	lcd.clear();
	lcd.setCursor(0, 1);
	lcd.print("L: ");
	lcd.print(lSpeed);
	lcd.print(" R: ");
	lcd.print(rSpeed);
	lcd.print("      ");
}

void setRightSpeed(int speed) {
	rSpeed = speed;
	if (speed < 0) {
		analogWrite(enB, -speed);
		digitalWrite(in3, LOW);
		digitalWrite(in4, HIGH);
	} else {
		analogWrite(enB, speed);
		digitalWrite(in3, HIGH);
		digitalWrite(in4, LOW);
	}
}

void setLeftSpeed(int speed) {
	lSpeed = speed;
	if (speed < 0) {
		analogWrite(enA, -speed);
		digitalWrite(in1, LOW);
		digitalWrite(in2, HIGH);
	} else {
		analogWrite(enA, speed);
		digitalWrite(in1, HIGH);
		digitalWrite(in2, LOW);
	}
}

void processBluetooth() {
	while (Serial.available() > 0) {
		String cmd = Serial.readStringUntil(';');
		int speed = cmd.substring(1, cmd.length()).toInt();
		Serial.println("Read command: speed = " + speed);
		if (cmd[0] == 'l') {
			setLeftSpeed(speed);
		} else if (cmd[0] == 'r') {
			setRightSpeed(speed);
		}

		delay(1);
	}
}

void detachInterrupts() {
	detachInterrupt(digitalPinToInterrupt(2));
	detachInterrupt(digitalPinToInterrupt(3));
}
void next() {
	if (lTurn == 0 && rTurn == 0) {
		detachInterrupts();
		setLeftSpeed(0);
		setRightSpeed(0);
	}
}

void l_count() {
	if (lTurn < 0) {
		lTurn++;
		setLeftSpeed(TURN_SPEED);
	} else if (lTurn > 0) {
		lTurn--;
		setLeftSpeed(-TURN_SPEED);
	}
	next();

}
void r_count() {
	if (rTurn < 0) {
		rTurn++;
		setRightSpeed(TURN_SPEED);
	} else if (rTurn > 0) {
		rTurn--;
		setRightSpeed(-TURN_SPEED);
	}
	next();
}
void attachInterrupts() {
	attachInterrupt(digitalPinToInterrupt(2), r_count, FALLING);
	attachInterrupt(digitalPinToInterrupt(3), l_count, FALLING);
}

void turnRight90() {
	rTurn = -TURN90;
	lTurn = TURN90;
	attachInterrupts();
	setLeftSpeed(TURN_SPEED);
	setRightSpeed(-TURN_SPEED);
}
void turnLeft90() {
	rTurn = TURN90;
	lTurn = -TURN90;
	attachInterrupts();
	setLeftSpeed(-TURN_SPEED);
	setRightSpeed(TURN_SPEED);
}

void processIR() {
	if (irrecv.decode(&results)) {
		Serial.println(results.value, HEX);
		switch (results.value) {
		case KEY_2:
			setLeftSpeed(255);
			setRightSpeed(255);
			break;
		case KEY_8:
			setLeftSpeed(-255);
			setRightSpeed(-255);
			break;
		case KEY_4:
			setLeftSpeed(-255);
			setRightSpeed(255);
			break;
		case KEY_6:
			setLeftSpeed(255);
			setRightSpeed(-255);
			break;
		case KEY_5:
			setLeftSpeed(0);
			setRightSpeed(0);
			break;
		case KEY_3:
			turnRight90();
			break;
		case KEY_1:
			turnLeft90();
			break;
		default:
			break;
		}

		irrecv.resume(); // Receive the next value
	}
}

void loop() {
//	if (counter++ > 360) {
//		counter = 0;
//	}

//		dispDistance();
	dispSpeed();

//	if (counter % 2 == 0) {
//		Serial.print(
//				String() + "f" + getDistance(frontTrigger, frontEcho) + "\n");
//	}
//	delay(50);

//	processBluetooth();
	processIR();
}
