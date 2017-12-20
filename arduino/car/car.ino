#include "Arduino.h"
#include <LiquidCrystal_I2C.h>
#include <MPU6050.h>
#include <SoftwareSerial.h>

const int BATTERY_PIN = A1;
const int EN_A = 10;
const int IN_1 = 8;
const int IN_2 = 9;

const int EN_B = 5;
const int IN_3 = 4;
const int IN_4 = 11; //from 3

const int FRONT_ECHO = 6;     // chân echo của HC-SR05
const int FRONT_TRIGGER = 7;     // chân trig của HC-SR05

int leftSpeed = 0;
int rightSpeed = 0;

int oldLeftSpeed = 0;
int oldRightSpeed = 0;
long pauseTimer = millis();
int pauseCounter = 0; //anti noise

double currentAngle = 0.0; //0-360 degrees
double desiredAngle = 0.0; //0-360 degrees
double remainingAngle = 0.0; //0-360 degrees

long lastTime = 0;
long microSecond = micros();
bool isStopping = true;

int maxSpeed = 200;
const int ROTATING_LEFT = 134;
const int ROTATING_RIGHT = 135;
int rotatingMode = ROTATING_LEFT;

const int MIN_DISTANCE = 20;
const int BLUETOOTH_RX = A2;
const int BLUETOOTH_TX = A3;
int count = 0;
const int MIN_ANGLE = 15;

bool isPause = false;

LiquidCrystal_I2C lcd(0x3F, 16, 2);    //the LCD
MPU6050 gyroscope;              //gyroscope
SoftwareSerial bluetooth(BLUETOOTH_RX, BLUETOOTH_TX);
String reason = "nope";


//get distance by cm from ultrasonic sensor
int getDistance(int trigger, int echo) {
    long duration, distanceCm;
    digitalWrite(trigger, LOW);
    delayMicroseconds(2);
    digitalWrite(trigger, HIGH);
    delayMicroseconds(5);
    digitalWrite(trigger, LOW);
    duration = pulseIn(echo, HIGH, 5000);
    distanceCm = duration * 340 / 20 / 1000;
    if (distanceCm == 0)
        return 99;
    return distanceCm;
}

//setup and init everything
void setup() {
    Serial.begin(9600);
    pinMode(EN_A, OUTPUT);
    pinMode(IN_1, OUTPUT);
    pinMode(IN_2, OUTPUT);
    pinMode(EN_B, OUTPUT);
    pinMode(IN_3, OUTPUT);
    pinMode(IN_4, OUTPUT);

    pinMode(BLUETOOTH_RX, INPUT);
    pinMode(BLUETOOTH_TX, OUTPUT);
    bluetooth.begin(9600);
    pinMode(BATTERY_PIN, INPUT);
    pinMode(FRONT_TRIGGER, OUTPUT);     // chân trig sẽ phát tín hiệu
    pinMode(FRONT_ECHO, INPUT);       // chân echo sẽ nhận tín hiệu



    lcd.begin();
    lcd.backlight();
    lcd.setCursor(0, 0);
    lcd.print("    NAT TEAM    ");
    lcd.setCursor(0, 1);
    lcd.print("AUTOCAR NAT TEAM");
    delay(500);
    while (!gyroscope.begin(MPU6050_SCALE_2000DPS, MPU6050_RANGE_2G)) {
        lcd.print("Not found MPU!!");
        delay(500);
    }
    gyroscope.calibrateGyro(10);
    lcd.clear();
}

void dispDistance(int row) {
    lcd.setCursor(0, row);
    lcd.print("F:");
    lcd.print(getDistance(FRONT_TRIGGER, FRONT_ECHO));
    if (millis() < pauseTimer) {
        lcd.print(" P:");
        lcd.print(pauseTimer - millis());
    }
}

const float MAX_BATTERY = 450;
const float MIN_BATTERY = 200;

void dispBattery(int row) {
    int sensorValue = analogRead(BATTERY_PIN); //read the A0 pin value
    float voltage = sensorValue * 5.0 * 5.24 / 1023.0;
    lcd.setCursor(0, row);
    lcd.print("Bat: ");
    lcd.print(voltage);
    lcd.print("V ");
    float battery; // phan tram pin
    battery = (sensorValue - MIN_BATTERY) * 100 / (MAX_BATTERY - MIN_BATTERY);
    lcd.print((int) battery);
    lcd.print("%          ");
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
    lcd.print(" S:");
    lcd.print(maxSpeed);
    lcd.print("      ");
}

void dispAngle(int row) {
    lcd.setCursor(0, row);
    if (isPause) {
        lcd.print("Pausing ");
        lcd.print((pauseTimer - millis()) / 1000);
        lcd.print(" ");
    } else if (isStopping) {
        lcd.print(" Stopping");
    } else {
        lcd.print("C:");
        lcd.print((int) currentAngle);
        lcd.print(" D:");
        lcd.print((int) desiredAngle);
    }
    lcd.print(" S:");
    lcd.print(maxSpeed);
    lcd.print("              ");
}

void setRightSpeed(int speed) {
    rightSpeed = speed;
    if (speed < 0) {
        analogWrite(EN_B, -speed);
        digitalWrite(IN_3, HIGH);
        digitalWrite(IN_4, LOW);
    } else {
        analogWrite(EN_B, speed);
        digitalWrite(IN_3, LOW);
        digitalWrite(IN_4, HIGH);
    }
}

void setLeftSpeed(int speed) {
    leftSpeed = speed;
    if (speed < 0) {
        analogWrite(EN_A, -speed);
        digitalWrite(IN_1, HIGH);
        digitalWrite(IN_2, LOW);
    } else {
        analogWrite(EN_A, speed);
        digitalWrite(IN_1, LOW);
        digitalWrite(IN_2, HIGH);
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
    gyroscope.calibrateGyro(1);
    stop("recalibrate");
}

void processBluetooth() {
    while (bluetooth.available() > 0) {
        String cmd = bluetooth.readStringUntil(';');
        int value = cmd.substring(1, cmd.length()).toInt();
        switch (cmd[0]) {
            case 'l':
                setLeftSpeed(value);
                isStopping = false;
                break;
            case 'r':
                setRightSpeed(value);
                isStopping = false;
                break;
            case 's':
                maxSpeed = value;
                break;
            case '_': //rotate
                desiredAngle = value;
                isStopping = false;
                break;
            case 'p': //pause
                stop("blue pause");
                return;
            case 'g': //gyro calibrate
                recalibrate();
                break;
            default:
                break;
        }
    }
    bluetooth.println(
            (String) "f" + getDistance(FRONT_TRIGGER, FRONT_ECHO) + ";;;c"
            + (int) currentAngle + ";;;d" + (int) desiredAngle + ";;;");

}

void processGyro() {
    Vector normGyro = gyroscope.readNormalizeGyro();
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


void processSpeed() {
    if (isStopping || isPause) {
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
//        remainingAngle = 360 - remainingAngle;
        setLeftSpeed(maxSpeed);
        setRightSpeed(-maxSpeed);
    } else {
        setLeftSpeed(maxSpeed);
        setRightSpeed(maxSpeed * 0.7);
    }
}


void pause() {
    isPause = true;
    oldLeftSpeed = leftSpeed;
    oldRightSpeed = rightSpeed;
    pauseTimer = millis() + 5000;
    setLeftSpeed(0);
    setRightSpeed(0);
}

void unPause() {
    isPause = false;
    setLeftSpeed(oldLeftSpeed);
    setRightSpeed(oldRightSpeed);
    pauseTimer = 0;
    pauseCounter = 0;
}


void processSonic() {
    int dis = getDistance(FRONT_TRIGGER, FRONT_ECHO);
    if ((dis <= MIN_DISTANCE)) {
        pauseCounter++;
        if (pauseCounter > 5)
            pause();
    } else if (pauseTimer > 0) {
        unPause();
    }
}

void processPause() {
    if (millis() > pauseTimer)
        processSonic();
}

void loop() {
    if (count++ > 10) {
        //those functions are very slow that can lower our FPS
        processBluetooth();
        dispAngle(1);
        dispBattery(0);
        count = 0;
    }
    processPause();
    processGyro();
    processSpeed();
}


