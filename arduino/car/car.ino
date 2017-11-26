#include "Arduino.h"
#include <LiquidCrystal_I2C.h>
#include <IRremote.h>
#include "IRKeyCode.h"
#include <MPU6050.h>
#include <time.h>
#include <math.h>
#include <SoftwareSerial.h>

const int IR_RECV_PIN = A1;
const int enA = 10;
const int in1 = 9;
const int in2 = 8;

const int enB = 5;
const int in3 = 4;
const int in4 = 11; //from 3

int lTurn = 0;
int rTurn = 0;

const int FRONT_ECHO = 6;     // chân echo của HC-SR05
const int FRONT_TRIGGER = 7;     // chân trig của HC-SR05
const int BACK_TRIGGER = 13;     // chân trig của HC-SR05
const int BACK_ECHO = 12;     // chân echo của HC-SR05

int leftSpeed = 0;
int rightSpeed = 0;

double currentAngle = 0.0; //0-360 degrees
double desiredAngle = 0.0; //0-360 degrees
double remainingAngle = 0.0; //0-360 degrees

long lastTime = 0;
long microSecond = micros();
bool isStopping = true;

int maxSpeed = 200;
const int STEP_SPEED = 10;
const int MIN_SPEED = 0;
const int ROTATING_LEFT = 134;
const int ROTATING_RIGHT = 135;
int rotatingMode = ROTATING_LEFT;

const int MIN_DISTANCE = 20;
const int BLUETOOTH_RX = A2;
const int BLUETOOTH_TX = A3;
int count = 0;

LiquidCrystal_I2C lcd(0x3F, 16, 2);		//the LCD
IRrecv irrecv(IR_RECV_PIN);				//the IR receiver
decode_results results;					//IR decode's result
MPU6050 mpu;							//gyroscope
SoftwareSerial bluetooth(BLUETOOTH_RX, BLUETOOTH_TX);
String reason = "nope";
int getDistance(int trigger, int echo) {
	return 30;
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

	pinMode(BLUETOOTH_RX, INPUT);
	pinMode(BLUETOOTH_TX, OUTPUT);
	bluetooth.begin(9600);

	pinMode(FRONT_TRIGGER, OUTPUT);   	// chân trig sẽ phát tín hiệu
	pinMode(BACK_TRIGGER, OUTPUT);   	// chân trig sẽ phát tín hiệu
	pinMode(FRONT_ECHO, INPUT);    		// chân echo sẽ nhận tín hiệu
	pinMode(BACK_ECHO, INPUT);    		// chân echo sẽ nhận tín hiệu

	while (!mpu.begin(MPU6050_SCALE_2000DPS, MPU6050_RANGE_2G)) {
		Serial.println("Not found MPU!!");
		delay(500);
	}

	lcd.begin();
	lcd.backlight();
	lcd.setCursor(0, 0);
	lcd.print("    NAT TEAM    ");
	lcd.setCursor(0, 1);
	lcd.print("AUTOCAR NAT TEAM");
	mpu.calibrateGyro(10);
	delay(500);
	lcd.clear();
}

void dispDistance(int row) {
	lcd.clear();
	lcd.setCursor(0, row);
	lcd.print("F: ");
	lcd.print(getDistance(FRONT_TRIGGER, FRONT_ECHO));
	lcd.print(" B: ");
	lcd.print(getDistance(BACK_TRIGGER, BACK_ECHO));
}

void dispBattery() { //not working
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

void dispSpeed(int row) {
	lcd.setCursor(0, row);
	if (isStopping)
		lcd.print("STOP " + reason);
	else {
		lcd.print("L:");
		lcd.print(leftSpeed);
		lcd.print(" R:");
		lcd.print(rightSpeed);
	}
	lcd.print(" M:");
	lcd.print(maxSpeed);
	lcd.print("      ");
}

void dispAngle(int row) {
	lcd.setCursor(0, row);
	lcd.print("C:");
	lcd.print(currentAngle);
	lcd.print(" R:");
	lcd.print((int) remainingAngle);
	lcd.print("   ");
}

void setRightSpeed(int speed) {
	rightSpeed = speed;
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
	leftSpeed = speed;
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

void stop(String _reason) {
	setRightSpeed(0);
	setLeftSpeed(0);
	isStopping = true;
	reason = _reason;
	Serial.println("Stop by " + reason);
	delay(1000);
}

void recalibrate() {
	currentAngle = 0;
	desiredAngle = 0;
	mpu.calibrateGyro(10);
	stop("recalibrate");
}

void processBluetooth() {
	Serial.println("avai");
	while (bluetooth.available() > 0) {
		Serial.println("until");
		String cmd = bluetooth.readStringUntil(';');
		int speed = cmd.substring(1, cmd.length()).toInt();
		switch (cmd[0]) {
		case 'l':
			setLeftSpeed(speed);
			isStopping = false;
			break;
		case 'r':
			setRightSpeed(speed);
			isStopping = false;
			break;
		case 's':
			maxSpeed = speed;
			break;
		case '_': //rotate
			desiredAngle = speed;
			isStopping = false;
			break;
		case 'p': //pause
			Serial.println("pause to read alll");
			stop("blue pause");
			return;
			break;
		case 'g': //gyro calibrate
			recalibrate();
			break;
		default:
			break;
		}
	}
	if (count == 9)
		bluetooth.println(
				(String) "f" + getDistance(FRONT_TRIGGER, FRONT_ECHO) + ";b"
						+ getDistance(BACK_TRIGGER, BACK_ECHO) + ";c"
						+ (int) currentAngle + ";d" + (int) desiredAngle + ";");
}

void processIR() {
	if (irrecv.decode(&results)) {
		switch (results.value) {
		case KEY_2:
			desiredAngle = 0;
			isStopping = false;
			break;
		case KEY_8:
			desiredAngle = 180;
			isStopping = false;
			break;
		case KEY_4:
			desiredAngle = 90;
			isStopping = false;
			break;
		case KEY_6:
			desiredAngle = -90;
			isStopping = false;
			break;
		case KEY_3:
			desiredAngle = -45;
			isStopping = false;
			break;
		case KEY_1:
			desiredAngle = 45;
			isStopping = false;
			break;
		case KEY_7:
			desiredAngle = 135;
			isStopping = false;
			break;
		case KEY_9:
			desiredAngle = -135;
			isStopping = false;
			break;
		case KEY_PLUS:
			if (maxSpeed < 256 - STEP_SPEED)
				maxSpeed += 10;
			break;
		case KEY_MINUS:
			if (maxSpeed > MIN_SPEED + STEP_SPEED)
				maxSpeed -= 10;
			break;
		case KEY_5:
			stop("key 5");
			break;
		case KEY_EQ:
			recalibrate();
			break;

		default:
			break;
		}
		irrecv.resume(); // Receive the next value
	}
}

void processGyro() {
	Vector normGyro = mpu.readNormalizeGyro();
	microSecond = micros();
	if (lastTime == 0) {
		lastTime = microSecond;
		return;
	} else {
		currentAngle += 1.0 * normGyro.XAxis * (microSecond - lastTime)
				/ 1000000;
		lastTime = microSecond;
	}
}

const int MIN_ANGLE = 5;
void processSpeed() {
	if (isStopping) {
		return;
	}
	remainingAngle = desiredAngle - currentAngle;
	while (remainingAngle < 0)
		remainingAngle += 360;
	while (remainingAngle > 360)
		remainingAngle -= 360;

	if (remainingAngle < MIN_ANGLE) {
		setRightSpeed(maxSpeed);
		setLeftSpeed(maxSpeed * 0.7);
	} else if (remainingAngle < 180) { //rotate left
		rotatingMode = ROTATING_LEFT;
		setRightSpeed(maxSpeed);
		setLeftSpeed(-maxSpeed);
	} else if (remainingAngle == 180) {
		if (rotatingMode == ROTATING_LEFT) {
			setRightSpeed(maxSpeed);
			setLeftSpeed(-maxSpeed);
		} else {
			setRightSpeed(-maxSpeed);
			setLeftSpeed(maxSpeed);
		}
	} else if (remainingAngle < (360 - MIN_ANGLE)) {
		rotatingMode = ROTATING_RIGHT;
		remainingAngle = 360 - remainingAngle;
		setLeftSpeed(maxSpeed);
		setRightSpeed(-maxSpeed);
	} else {
		setLeftSpeed(maxSpeed);
		setRightSpeed(maxSpeed * 0.7);
	}
}

void processSonic() {
	if ((getDistance(FRONT_TRIGGER, FRONT_ECHO) <= MIN_DISTANCE)
//			|| (getDistance(BACK_TRIGGER, BACK_ECHO) <= MIN_DISTANCE)
	)
		stop("sonic");
}

void profile(void* func, String name) {
	long start = micros();
	((void (*)()) func)();
	long delta = micros() - start;
	Serial.println("Function " + name + " runs in " + delta + " micros");
}

void main_loop() {
	if (count++ > 5) {
		dispSpeed(0);
		dispAngle(1);
		processSonic();
		count = 0;
	}
	processBluetooth(); //those functions are very slow that can lower our FPS
	processIR();
	processGyro();
	processSpeed();
}

long fps_timer = micros();
int fps_counter = 0;
void loop() {
	main_loop();
	if (micros() - fps_timer > 1000000) {
		fps_timer = micros();
		Serial.println((String) "FPS: " + fps_counter);
		fps_counter = 0;
	}
	fps_counter++;
}

