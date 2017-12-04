package com.worker.natrobotcontroller.models;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.worker.natrobotcontroller.util.MatUtil;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.COLOR_RGBA2GRAY;

public final class TrafficSign {

    public static final int STRAIGHT = 0;
    public static final int BACK = 2;
    public static final int LEFT = 1;
    public static final int RIGHT = 3;
    public static final int STOP = -1;
    public Mat img;
    public final int direction;
    public String msg;

    public TrafficSign(String _msg, Activity mainActivity, int res_id, int direction) {
        this.direction = direction;
        this.img = new Mat();
        this.msg = _msg;
        Bitmap bm = BitmapFactory.decodeResource(mainActivity.getResources(), res_id);
        Mat temp = new Mat();
        Utils.bitmapToMat(bm, temp);
        Imgproc.cvtColor(temp, this.img, COLOR_RGBA2GRAY); //convert from 4 channels full color to 1 channel grayscale
        this.img = MatUtil.toBinaryMat(img);
        temp.release();
        bm.recycle();
    }


}
