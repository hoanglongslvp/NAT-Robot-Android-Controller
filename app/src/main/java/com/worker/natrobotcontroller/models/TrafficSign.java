package com.worker.natrobotcontroller.models;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public final class TrafficSign {
    @NotNull
    private final Mat img;
    @NotNull
    private String msg;
    private final int direction;
    public static final int STRAIGHT = 0;
    public static final int BACK = 2;
    public static final int LEFT = 1;
    public static final int RIGHT = 3;
    public static final int STOP = -1;

    @NotNull
    public final Mat getImg() {
        return this.img;
    }

    @NotNull
    public final String getMsg() {
        return this.msg;
    }

    public final void setMsg(@NotNull String var1) {
        this.msg = var1;
    }

    public final int getDirection() {
        return this.direction;
    }

    public TrafficSign(@NotNull String _msg, @NotNull Activity mainActivity, int res_id, int direction) {
        this.direction = direction;
        this.img = new Mat();
        this.msg = "";
        this.msg = _msg;
        Bitmap bm = BitmapFactory.decodeResource(mainActivity.getResources(), res_id);
        Mat temp = new Mat();
        Utils.bitmapToMat(bm, temp);
        Imgproc.cvtColor(temp, this.img, 11);
        temp.release();
        bm.recycle();
    }


}
