#include <LiquidCrystal_I2C.h>
#include <Wire.h>

LiquidCrystal_I2C lcd(0x3F,16,2);

#define enA 10
#define in1 9
#define in2 8
#define enB 5
#define in3 4
#define in4 3

byte blue = 0;//Bien nhan du lieu bluetooth

//#define TRIG_PIN 13
//#define ECHO_PIN 12
//
//const int trig = 7;     // chân trig của HC-SR05
//const int echo = 6;     // chân echo của HC-SR05
//
//#define TIME_OUT 5000

//float GetDistance()
//{
//  long duration, distanceCm;
//   
//  digitalWrite(TRIG_PIN, LOW);
//  delayMicroseconds(2);
//  digitalWrite(TRIG_PIN, HIGH);
//  delayMicroseconds(10);
//  digitalWrite(TRIG_PIN, LOW);
//  
//  duration = pulseIn(ECHO_PIN, HIGH, TIME_OUT);
// 
//  // convert to distance
//  distanceCm = duration / 29.1 / 2;
//  
//  return distanceCm;
//}
void setup() {
  // put your setup code here, to run once:
  //khởi tạo blue + chan dong co
  Serial.begin(9600);
  pinMode( enA, OUTPUT );
  pinMode( in1, OUTPUT );
  pinMode( in2, OUTPUT );
  pinMode( enB, OUTPUT );
  pinMode( in3, OUTPUT );
  pinMode( in4, OUTPUT );

//  pinMode(TRIG_PIN, OUTPUT);
//  pinMode(ECHO_PIN, INPUT);
//
//  pinMode(trig,OUTPUT);   // chân trig sẽ phát tín hiệu
//  pinMode(echo,INPUT);    // chân echo sẽ nhận tín hiệu

  lcd.init();
  lcd.backlight();  
  lcd.setCursor(0,0);
  lcd.print("    NAT TEAM    ");
  lcd.setCursor(0,1); 
  lcd.print("AUTOCAR NAT TEAM"); 
  delay(2000);
  lcd.clear();
}

void tien(){
  analogWrite( enA, 150 );
  digitalWrite( in1, HIGH );
  digitalWrite( in2, LOW );
  analogWrite( enB, 150 );
  digitalWrite( in3, HIGH );
  digitalWrite( in4, LOW );
  }

void lui(){
  analogWrite( enA, 150 );
  digitalWrite( in1, LOW );
  digitalWrite( in2, HIGH );
  analogWrite( enB, 150 );
  digitalWrite( in3, LOW );
  digitalWrite( in4, HIGH );
  }

void rePhai(){
  analogWrite( enA, 200 );
  digitalWrite( in1, HIGH );
  digitalWrite( in2, LOW );
  analogWrite( enB, 200 );
  digitalWrite( in3, LOW );
  digitalWrite( in4, HIGH );
  }

void reTrai(){
  analogWrite( enA, 200 );
  digitalWrite( in1, LOW );
  digitalWrite( in2, HIGH );
  analogWrite( enB, 200 );
  digitalWrite( in3, HIGH );
  digitalWrite( in4, LOW );
  }

void dung(){
  digitalWrite( in1, LOW );
  digitalWrite( in2, LOW );
  digitalWrite( in3, LOW );
  digitalWrite( in4, LOW );
  }

//void tangToc(){
//  int i;
//  for( i=0;i<256;i++ ){
//    analogWrite( enA, i );
//    digitalWrite( in1, HIGH );
//    digitalWrite( in2, LOW );
//    analogWrite( enB, i );
//    digitalWrite( in3, HIGH );
//    digitalWrite( in4, LOW );
//    delay(1500);
//    }
//    digitalWrite( in1, LOW );
//    digitalWrite( in2, LOW );
//    digitalWrite( in3, LOW );
//    digitalWrite( in4, LOW );
//  }

void loop() {
  unsigned long duration1; // biến đo thời gian
  int distance1;           // biến lưu khoảng cách
  long distance = GetDistance();
    digitalWrite(trig,0);   // tắt chân trig
    delayMicroseconds(2);
    digitalWrite(trig,1);   // phát xung từ chân trig
    delayMicroseconds(5);   // xung có độ dài 5 microSeconds
    digitalWrite(trig,0);   // tắt chân trig
    // Đo độ rộng xung HIGH ở chân echo. 
    duration1 = pulseIn(echo,HIGH);  
    // Tính khoảng cách đến vật.
    distance1 = int(duration1/2/29.1);

    Serial.print("Khoang cach Truoc (cm): ");
    Serial.println(distance1);
    Serial.print("Khoang cach Sau (cm): ");
    Serial.println(distance);
    delay(50);

    //Tinh toan Voltage
    int sensorValue = analogRead(A0); //read the A0 pin value
    float voltage = sensorValue * (5.0 / 1023.0);
    //Serial.println(voltage);
    lcd.setCursor(0,0);
    lcd.print("Voltage = ");
    lcd.print(voltage);
    lcd.print(" V");
    int battery; // phan tram pin
    battery = ((sensorValue * (5.00 / 1023.00)) / 5 * 100);
    lcd.setCursor(0,1);
    lcd.print("Battery = ");
    lcd.print(battery);
    lcd.print("%");
    
//  if (distance1 <= 35){
//    rePhai();
//    if (distance1 ==0){
//      tien();
//    }
//   } else {
//      tien();
//   }
//Robot_move
    if( Serial.available()>0 )
      {
        blue = Serial.read();
        Serial.println(blue);
      }
    if( blue == 1 )//di thang
      {
        Serial.println("Move Forward");
        tien();
        blue=0;
        delay(5);
      }
     if( blue == 2 )//lui
      {
        Serial.println("Move Backward");
        lui();
        blue=0;
        delay(5);
      }
     if( blue == 3 )//re trai
      {
        Serial.println("Rotate Left");
        reTrai();
        blue=0;
        delay(5);
      }
     if( blue == 4 )//re Phai
      {
        Serial.println("Rotate Right");
        rePhai();
        blue=0;
        delay(5);
      }
      if( blue == 6 )//Dung dong co
      {
        Serial.println("STOP");
        dung();
        blue=0;
        delay(5);
      }

}
