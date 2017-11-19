package com.worker.natrobotcontroller.models

import android.app.Activity
import android.graphics.BitmapFactory
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.COLOR_RGBA2GRAY

/**
 * Created by hataketsu on 11/11/17.
 */
class TrafficSign(_msg: String, mainActivity: Activity, res_id: Int) {
    val img = Mat()
    var msg = ""

    init {
        msg = _msg
        val bm = BitmapFactory.decodeResource(mainActivity.resources, res_id)
        val temp = Mat()
        Utils.bitmapToMat(bm, temp)
        Imgproc.cvtColor(temp, img, COLOR_RGBA2GRAY)
        temp.release()
        bm.recycle()
    }

}