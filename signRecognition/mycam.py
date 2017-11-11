import cv2
cap=cv2.VideoCapture("/home/hataketsu/Videos/SUPER-HERO-BOWL! - TOON SANDWICH.mp4")
while True:
    _,frame=cap.read()
    cv2.imshow('video',frame)
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break
cap.release()
cv2.destroyAllWindows()
