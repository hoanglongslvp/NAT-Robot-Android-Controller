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
    public static void draw(@NotNull RotatedRect $receiver, @NotNull Mat mat, @NotNull Scalar color) {
        Point[] vertices = new Point[4];
        $receiver.points(vertices);
        int j = 0;

        for(byte var5 = 4; j < var5; ++j) {
            Imgproc.line(mat, vertices[j], vertices[(j + 1) % 4], color);
        }

    }

    public static void fixRotation(@NotNull RotatedRect $receiver) {
        if($receiver.angle < -45.0D) {
            $receiver.angle += 90.0D;
            double tmp = $receiver.size.width;
            $receiver.size.width = $receiver.size.height;
            $receiver.size.height = tmp;
        }

    }
}
