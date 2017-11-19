package com.worker.natrobotcontroller.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.worker.natrobotcontroller.R
import com.worker.natrobotcontroller.models.MatchSignResult
import com.worker.natrobotcontroller.models.TrafficSign
import kotlinx.android.synthetic.main.camera_sight.view.*
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * Created by hataketsu on 11/12/17.
 */
class CameraSightFragment : Fragment() {
    var enableCamera = false
    var initedOpenCV = false
    var drawRect = false
    var drawContours = false
    var drawError = false
    var drawThumbnail = false
    var isStreaming = false
    var cameraSize = "Fullsize"
    var detectThreshold = 1.5
    val detectColor = Scalar(25.0, 118.0, 210.0);
    val selectColor = Scalar(255.0, 64.0, 129.0);
    val matStack = mutableListOf<Mat>()
    val signs = mutableListOf<TrafficSign>()
    var bestResult: MatchSignResult? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.camera_sight, container, false)
        setupView(v)
        return v
    }

    private fun setupView(v: View) {
        v.cameraView.setCvCameraViewListener(object : CameraBridgeViewBase.CvCameraViewListener2 {
            override fun onCameraViewStarted(width: Int, height: Int) {}

            override fun onCameraViewStopped() {}

            override fun onCameraFrame(frame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
                matStack.forEach { it.release() }
                matStack.clear()

                val rgba = frame.rgba()
                matStack.add(rgba)
                val mask = getColorMask(rgba)
                val contours = getContours(mask)
                bestResult = null
                if (contours.size > 0) { //we got a sign
                    if (drawContours)
                        Imgproc.drawContours(rgba, contours, -1, detectColor)

                    contours.forEach { contour ->
                        val rect = getMinAreaRect(contour)
                        if (rect.size.width > 20 && rect.size.height > 20) {
                            if (drawRect)
                                drawRotatedRect(rect, rgba, detectColor)

                            fixRotationRect(rect)
                            //get rotation matrix from the rotated rectangle
                            val rotationMatrix2D = Imgproc.getRotationMatrix2D(rect.center, rect.angle, 1.0)
                            matStack.add(rotationMatrix2D)
                            val thumbnail = resizeMat(getThumbnail(mask, rect, rotationMatrix2D), 50, 50)

                            val best = getBestMatchSign(thumbnail, rect)
                            if (best != null) {
                                if (bestResult == null)
                                    bestResult = best
                                else if (best.error < bestResult!!.error)
                                    bestResult = best
                                if (drawError)
                                    Imgproc.putText(rgba, "Match : " + best.sign.msg + " : " + best.error, Point(20.0, 100.0), Core.FONT_HERSHEY_SIMPLEX, 0.65, selectColor, 2)
                            }
                        }
                        contour.release()
                    }

                    if (bestResult != null) {
                        if (drawRect) {
                            drawRotatedRect(bestResult!!.rect, rgba, selectColor)
                        }
                        if (drawThumbnail) {
                            val cropped = Mat()
                            matStack.add(cropped)
                            Imgproc.cvtColor(bestResult!!.thumbnail, cropped, Imgproc.COLOR_GRAY2RGBA)
                            cropped.copyTo(Mat(rgba, Rect(20, 20, cropped.width(), cropped.height())))
                        }
                    }
                }
                return rgba
            }

        })
    }

    private fun getBestMatchSign(thumbnail: Mat, rect: RotatedRect): MatchSignResult? {
        var sign: TrafficSign? = null
        var error = 100000000.0
        for (s in signs) {
            val e = getError(s.img, thumbnail)
            if (e < error) {
                sign = s
                error = e
            }
            Log.d("Error", s.msg + " : " + e)
        }

        Log.d("Error", "Best " + (sign?.msg))
        if (sign != null && error < detectThreshold) {
            return MatchSignResult(sign, error, thumbnail, rect)
        }
        return null
    }

    private fun getThumbnail(mask: Mat, rect: RotatedRect, rotationMatrix2D: Mat?): Mat {
        val rotatedMask = Mat()
        Imgproc.warpAffine(mask, rotatedMask, rotationMatrix2D, mask.size(), Imgproc.INTER_CUBIC)
        val cropped = Mat()
        Imgproc.getRectSubPix(rotatedMask, rect.size, rect.center, cropped)
        Core.bitwise_not(cropped, cropped)
        rotatedMask.release()
        return cropped
    }

    val openCVInitCallback: LoaderCallbackInterface = object : LoaderCallbackInterface {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i("INIT", "OpenCV loaded successfully")
                    initedOpenCV = true
                    preloadImage()
                    switchCamera()
                }
                else -> Log.w("INIT", "OpenCV loading failed")

            }
        }

        override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {}

    }

    private fun preloadImage() {
        signs.add(TrafficSign("Turn right", activity, R.drawable.turnright))
        signs.add(TrafficSign("Turn left", activity, R.drawable.turnleft))
        signs.add(TrafficSign("Turn back", activity, R.drawable.turnback))
        signs.add(TrafficSign("Move straight", activity, R.drawable.movestraight))
    }


    override fun onResume() {
        super.onResume()
        reloadSetting()
        Log.d("Camerasight", "Resume")
        if (!OpenCVLoader.initDebug()) {
            Log.d("INIT", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, activity, openCVInitCallback);
        } else {
            Log.d("INIT", "OpenCV library found inside package. Using it!");
            openCVInitCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private fun reloadSetting() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        drawRect = preferences.getBoolean("is_draw_rectangles", true)
        drawContours = preferences.getBoolean("is_draw_contours", true)
        drawError = preferences.getBoolean("is_draw_error", true)
        drawThumbnail = preferences.getBoolean("is_draw_thumbnail", true)
        isStreaming = preferences.getBoolean("is_streaming", false)
        detectThreshold = preferences.getString("detect_threshold", "1.5").toDouble()
        cameraSize = preferences.getString("camera_size", "Fullsize")

//        val _detectColor = preferences.getLong("detect_color", resources.getColor(R.color.detect).toLong())
//        val _selectColor = preferences.getLong("detect_color", resources.getColor(R.color.select).toLong())

    }

    //fix the rotated angle
    private fun fixRotationRect(rect: RotatedRect) {
        if (rect.angle < -45.0) {
            rect.angle += 90.0
            val tmp = rect.size.width //swap width and height
            rect.size.width = rect.size.height
            rect.size.height = tmp
        }
    }

    //show a captured thumb of traffic sign
    private fun drawColorThumbnail(rgba: Mat, rotationMatrix2D: Mat?, rect: RotatedRect) {
        val rotatedMat = Mat()
        Imgproc.warpAffine(rgba, rotatedMat, rotationMatrix2D, rgba.size(), Imgproc.INTER_CUBIC)
        val cropped = Mat()

        val rotatedChannels = mutableListOf<Mat>()
        val croppedChannels = mutableListOf<Mat>()
        val tempChannel = Mat()
        Core.split(rotatedMat, rotatedChannels)
        for (channel in rotatedChannels) {
            Imgproc.getRectSubPix(channel, rect.size, rect.center, tempChannel)
            croppedChannels.add(tempChannel.clone())
        }
        Core.merge(croppedChannels, cropped)
        cropped.copyTo(Mat(rgba, Rect(20, 20, cropped.width(), cropped.height())))

        rotatedChannels.forEach { it.release() } //must release all elements in the array to prevent memory leak
        croppedChannels.forEach { it.release() }
        tempChannel.release()
        rotatedMat.release()
        cropped.release()
    }

    private fun drawMaskThumbnail(rgba: Mat, rotationMatrix2D: Mat?, rect: RotatedRect, mask: Mat) {
        val rotatedMat = Mat()
        Imgproc.warpAffine(mask, rotatedMat, rotationMatrix2D, rgba.size(), Imgproc.INTER_CUBIC)
        var cropped = Mat()
        Imgproc.getRectSubPix(rotatedMat, rect.size, rect.center, cropped)
        Core.bitwise_not(cropped, cropped)
        var rgbaCropped = Mat()
        Imgproc.cvtColor(cropped, rgbaCropped, Imgproc.COLOR_GRAY2RGBA)
        rgbaCropped = resizeMat(rgbaCropped, 50, 50)
        cropped = resizeMat(cropped, 50, 50)
        rgbaCropped.copyTo(Mat(rgba, Rect(20, 20, rgbaCropped.width(), rgbaCropped.height())))
        var sign: TrafficSign? = null
        var similary = 100000000.0
        for (s in signs) {
            val err = getError(s.img, cropped)
            if (err < similary) {
                sign = s
                similary = err
            }
            Log.d("Error", s.msg + " : " + err)
        }

        Log.d("Error", "Best " + (sign?.msg))
        if (sign != null) {
            if (similary < 1.5)
                Imgproc.putText(rgba, "Match : " + sign.msg + " : " + similary, Point(20.0, 100.0), Core.FONT_HERSHEY_SIMPLEX, 0.65, Scalar(0.0, 255.0, 0.0), 2)
        }
        rgbaCropped.release()
        rotatedMat.release()
        cropped.release()
    }

    fun getError(A: Mat, B: Mat): Double {
        if (A.rows() > 0 && A.rows() == B.rows() && A.cols() > 0 && A.cols() == B.cols()) {
            // Calculate the L2 relative error between images.
            val errorL2 = Core.norm(A, B, Core.NORM_L2);
            // Convert to a reasonable scale, since L2 error is summed across all pixels of the image.
            val error = errorL2 / (A.rows() * A.cols());
            return error;
        } else {
            //Images have a different size
            return 100000000.0;  // Return a bad value
        }
    }

    private fun resizeMat(mat: Mat, w: Int, h: Int): Mat {
        val result = Mat()
        matStack.add(result)
        Imgproc.resize(mat, result, Size(w.toDouble(), h.toDouble()))
        return result
    }


    private fun getMinAreaRect(contour: MatOfPoint): RotatedRect {
        val matOfPoint2f = MatOfPoint2f()
        contour.convertTo(matOfPoint2f, CvType.CV_32F)
        val result = Imgproc.minAreaRect(matOfPoint2f)
        matOfPoint2f.release()
        return result
    }

    //get the biggest contour from a set of contours
    private fun getBiggestContour(contours: MutableList<MatOfPoint>): Int {
        var biggest = 0.0
        var index = -1
        for (i in 0..(contours.size - 1)) {
            val area = Imgproc.contourArea(contours[i])
            if (area > biggest) {
                biggest = area
                index = i
            }
        }
        return index
    }

    //get contours from a binary image
    private fun getContours(mask: Mat): MutableList<MatOfPoint> {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        matStack.add(hierarchy)
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, Point(0.0, 0.0))
        return contours
    }

    //generate a binary image from a full color image
    private fun getColorMask(rgba: Mat): Mat {
        val hsv = Mat()
        matStack.add(hsv)
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV, 3) //convert rgb image to hsv image
        val mask = Mat()
        matStack.add(mask)
        val lower_color = Scalar(160.0 / 2, 100.0, 70.0) //the lower color limit
        val upper_color = Scalar(277.0 / 2, 255.0, 255.0) //the upper color limit
        Core.inRange(hsv, lower_color, upper_color, mask) //get all a pixel that stays between two limits
        return mask
    }

    private fun drawRotatedRect(rRect: RotatedRect, mat: Mat, color: Scalar) {
        val vertices = arrayOfNulls<Point>(4)
        rRect.points(vertices)
        for (j in 0..3) {
            Imgproc.line(mat, vertices[j], vertices[(j + 1) % 4], color)
        }
    }


    private fun switchCamera() {
        if (initedOpenCV) //must be sure that openCV has been initialized before enabling Camera
            if (enableCamera) {
                reloadSetting()
                view?.cameraView?.enableView()
            } else
                view?.cameraView?.disableView()
        when (cameraSize) {
            "Fullsize" -> setSize(GONE, GONE, GONE, GONE)
            "Small" -> setSize(VISIBLE, VISIBLE, VISIBLE, VISIBLE)
            "Medium" -> setSize(VISIBLE, GONE, VISIBLE, GONE)
        }
    }


    private fun setSize(f: Int, s: Int, t: Int, l: Int) {
        view!!.first.visibility = f
        view!!.second.visibility = s
        view!!.third.visibility = t
        view!!.last.visibility = l
    }

    fun switchCam(enable: Boolean) {
        enableCamera = enable
        Log.d("switchcam", "called with " + enable)
        switchCamera()
    }
}