package com.worker.natrobotcontroller.util;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public final class MatUtil {
    public static void draw(@NotNull RotatedRect rect, @NotNull Mat mat, @NotNull Scalar color) {
        Point[] vertices = new Point[4];
        rect.points(vertices);
        int j = 0;
        for(byte var5 = 4; j < var5; ++j) {
            Imgproc.line(mat, vertices[j], vertices[(j + 1) % 4], color);
        }

    }

    public static void fixRotation(@NotNull RotatedRect rect) {
        if(rect.angle < -45.0D) {
            rect.angle += 90.0D;
            double tmp = rect.size.width;
            rect.size.width = rect.size.height;
            rect.size.height = tmp;
        }
    }
}
