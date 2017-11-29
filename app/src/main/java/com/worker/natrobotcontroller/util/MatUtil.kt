package com.worker.natrobotcontroller.util

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.RotatedRect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

/**
 * Created by hataketsu on 11/28/17.
 */

fun RotatedRect.draw(mat: Mat, color: Scalar) {
    val vertices = arrayOfNulls<Point>(4)
    this.points(vertices)
    for (j in 0..3) {
        Imgproc.line(mat, vertices[j], vertices[(j + 1) % 4], color)
    }
}

//fix the rotated angle
 fun RotatedRect.fixRotation() {
    if (angle < -45.0) {
        angle += 90.0
        val tmp = size.width //swap width and height
        size.width = size.height
        size.height = tmp
    }
}