package com.worker.natrobotcontroller.util;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

public final class MatUtil {
    public static void draw(@NotNull RotatedRect rect, @NotNull Mat mat, @NotNull Scalar color) {
        Point[] vertices = new Point[4];
        rect.points(vertices);
        int j = 0;
        for (byte var5 = 4; j < var5; ++j) {
            Imgproc.line(mat, vertices[j], vertices[(j + 1) % 4], color);
        }

    }

    public static void fixRotation(@NotNull RotatedRect rect) {
        if (rect.angle < -45.0D) {
            rect.angle += 90.0D;
            double tmp = rect.size.width;
            rect.size.width = rect.size.height;
            rect.size.height = tmp;
        }
    }

    public static void logMat(String _msg, Mat img) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < img.size().height; i++) {
            for (int j = 0; j < img.size().width; j++) {
                buf.append(img.get(i, j)[0]).append(",");
            }
            buf.append("\n");
        }
        Log.d("IMAGES", _msg + buf.toString());
    }

    public static Mat toBinaryMat(Mat org) {
        Mat tmp = new Mat();
        Imgproc.threshold(org, tmp, 127, 255, THRESH_BINARY);
        org.release();
        return tmp;
    }
}
