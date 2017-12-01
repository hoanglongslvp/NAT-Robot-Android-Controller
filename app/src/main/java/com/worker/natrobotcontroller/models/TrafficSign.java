package com.worker.natrobotcontroller.models;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.COLOR_RGBA2GRAY;

public final class TrafficSign {
    
    public final Mat img;
    public String msg;
    public final int direction;
    public static final int STRAIGHT = 0;
    public static final int BACK = 2;
    public static final int LEFT = 1;
    public static final int RIGHT = 3;
    public static final int STOP = -1;

    public TrafficSign( String _msg,  Activity mainActivity, int res_id, int direction) {
        this.direction = direction;
        this.img = new Mat();
        this.msg = _msg;
        Bitmap bm = BitmapFactory.decodeResource(mainActivity.getResources(), res_id);
        Mat temp = new Mat();
        Utils.bitmapToMat(bm, temp);
        Imgproc.cvtColor(temp, this.img, COLOR_RGBA2GRAY);
        temp.release();
        bm.recycle();
    }


}
