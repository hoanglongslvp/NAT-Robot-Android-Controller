import cv2,imutils,numpy as np
from imutils.perspective import *

def polygon(img,points):
    _points=list( points)
    _points.append(points[0])
    for i,j in enumerate(points):
        cv2.line(img,tuple(j),tuple(_points[i+1]),(0,255,0),1)

def show_and_save(name,img):
    cv2.imshow(name, img)
    cv2.imwrite(name+".jpg", img)

origin=cv2.imread("./img.jpg")
origin=imutils.resize(origin,width=500)
show_and_save("01origin",origin)
lower_blue = np.array([85, 100, 70])
upper_blue = np.array([115, 255, 255])
hsv = cv2.cvtColor(origin, cv2.COLOR_BGR2HSV)
show_and_save("02hsv",hsv)
mask = cv2.inRange(hsv, lower_blue, upper_blue)
show_and_save("03mask", mask)
cnts = cv2.findContours(
            mask.copy(), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)[-2]
contours=origin.copy()
cv2.drawContours(contours,cnts,-1,(0, 0, 255),1)
show_and_save("04contours", contours)
cv2.drawContours(contours,cnts,-1,(0, 0, 255),2)
show_and_save("04contours2", contours)
mask2=cv2.cvtColor(mask,cv2.COLOR_GRAY2BGR)
cv2.drawContours(mask2,cnts,-1,(200, 200, 80),1)
show_and_save("045mask_contours",mask2)
cv2.drawContours(mask2,cnts,-1,(200, 200, 80),2)
show_and_save("045mask_contours2",mask2)

rects=origin.copy()
p=None
for j,i in enumerate(cnts):
    rect=cv2.minAreaRect(i)
    points=cv2.boxPoints(rect)
    p=points
    polygon(rects,points)

show_and_save("05rects", rects)
cropped = four_point_transform(mask, p)
show_and_save("06cropped", cropped)


show_and_save("07inverted_cropped",cv2.bitwise_not(cropped,cropped))

cv2.waitKey()