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
import android.widget.SeekBar
import com.worker.natrobotcontroller.R
import com.worker.natrobotcontroller.activities.MainActivity
import com.worker.natrobotcontroller.models.MatchSignResult
import com.worker.natrobotcontroller.models.TrafficSign
import com.worker.natrobotcontroller.util.draw
import com.worker.natrobotcontroller.util.fixRotation
import kotlinx.android.synthetic.main.camera_sight.view.*
import org.jetbrains.anko.toast
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * Created by hataketsu on 11/12/17.
 */
class CameraSightFragment : Fragment() {
    var enableCamera = false
    var initedOpenCV = false
    var isDebug = false
    var cameraSize = "Fullsize"
    var detectThreshold = 1.5
    val detectColor = Scalar(25.0, 118.0, 210.0)
    val selectColor = Scalar(255.0, 64.0, 129.0)
    val matStack = mutableListOf<Mat>()
    val signs = mutableListOf<TrafficSign>()
    var bestResult: MatchSignResult? = null
    var signMatchSize = 0
    val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

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
                    if (isDebug)
                        Imgproc.drawContours(rgba, contours, -1, detectColor)

                    contours.forEach { contour ->
                        val rect = getMinAreaRect(contour)
                        if (rect.size.height > signMatchSize /*&& (0.8 < rect.size.width / rect.size.height) && (rect.size.width / rect.size.height < 1.2)*/) {
                            if (isDebug)
                                rect.draw( rgba, detectColor)

                            rect.fixRotation()
                            //get rotation matrix from the rotated rectangle
                            val rotationMatrix2D = Imgproc.getRotationMatrix2D(rect.center, rect.angle, 1.0)
                            matStack.add(rotationMatrix2D)
                            val thumbnail = resizeMat(getThumbnail(mask, rect, rotationMatrix2D), 50, 50)

                            val best = getBestMatchSign(thumbnail, rect)
                            if (best != null) {
                                if (bestResult == null)
                                    bestResult = best
                                else if (best.rect.size.area() > bestResult!!.rect.size.area())
                                    bestResult = best
                                if (isDebug)
                                    Imgproc.putText(rgba, best.sign.msg + ":" + best.error, bestResult!!.rect.center, Core.FONT_HERSHEY_SIMPLEX, 0.65, detectColor, 2)
                            }
                        }
                        contour.release()
                    }

                    if (bestResult != null) {
                        bestResult!!.rect.draw( rgba, selectColor)
                        trigger(bestResult!!.sign.direction)
                        Imgproc.putText(rgba, bestResult!!.sign.msg, bestResult!!.rect.center, Core.FONT_HERSHEY_SIMPLEX, 0.65, selectColor, 2)

                        if (isDebug) {
                            val cropped = Mat()
                            matStack.add(cropped)
                            Imgproc.cvtColor(bestResult!!.thumbnail, cropped, Imgproc.COLOR_GRAY2RGBA)
                            cropped.copyTo(Mat(rgba, Rect(20, 20, cropped.width(), cropped.height())))
                        }
                    }

                }
                drawSignSizeIndicator(rgba)
                return rgba
            }

        })
        v.signSizeBar.max = v.cameraView.height
        v.signSizeBar.progress = signMatchSize
        v.signSizeBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(p0: SeekBar?, progress: Int, isUser: Boolean) {
                        signMatchSize = progress
                    }
                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        preferences.edit().putInt("sign_size", signMatchSize).apply()
                    }
                })
    }

    private fun trigger(direction: Int) {
        (activity as MainActivity).controller?.trigger(direction)
    }

    private fun drawSignSizeIndicator(rgba: Mat) {
        Imgproc.line(rgba, Point(rgba.width().toDouble(), 10.0), Point(rgba.width().toDouble() - 20, 10.0), selectColor, 2)
        Imgproc.line(rgba, Point(rgba.width().toDouble(), 10.0 + signMatchSize), Point(rgba.width().toDouble() - 20, 10.0 + signMatchSize), selectColor, 2)
    }

    private fun getBestMatchSign(thumbnail: Mat, rect: RotatedRect): MatchSignResult? {
        var sign: TrafficSign? = null
        var diff = 100000000.0
        for (s in signs) {
            val d = measureDifferenceBetween(s.img, thumbnail)
            if (d < diff) {
                sign = s
                diff = d
            }
        }
        if (sign != null && diff < detectThreshold) {
            return MatchSignResult(sign, diff, thumbnail, rect)
        } else
            return null
    }

    private fun getThumbnail(mask: Mat, rect: RotatedRect, rotationMatrix2D: Mat?): Mat {
        val rotatedMask = Mat()
        matStack.add(rotatedMask)
        Imgproc.warpAffine(mask, rotatedMask, rotationMatrix2D, mask.size(), Imgproc.INTER_CUBIC)
        val cropped = Mat()
        matStack.add(cropped)
        Imgproc.getRectSubPix(rotatedMask, rect.size, rect.center, cropped)
        Core.bitwise_not(cropped, cropped)
        return cropped
    }

    private fun preloadImage() {
        signs.add(TrafficSign("Turn right", activity, R.drawable.turnright, TrafficSign.RIGHT))
        signs.add(TrafficSign("Turn left", activity, R.drawable.turnleft, TrafficSign.LEFT))
        signs.add(TrafficSign("Turn back", activity, R.drawable.turnback, TrafficSign.BACK))
        signs.add(TrafficSign("Move straight", activity, R.drawable.movestraight, TrafficSign.STRAIGHT))
    }

    //measure difference between two same-size images, lower difference is better
    private fun measureDifferenceBetween(A: Mat, B: Mat): Double {
        if (A.rows() > 0 && A.rows() == B.rows() && A.cols() > 0 && A.cols() == B.cols()) {
            val errorL2 = Core.norm(A, B, Core.NORM_L2) // Calculate the L2 relative error between images.
            val error = errorL2 / (A.rows() * A.cols()) // Convert to a reasonable scale, since L2 error is summed across all pixels of the image.
            return error;
        } else {
            //Images have a different size
            return 100000000.0;  // Return a bad value
        }
    }

    //resize image to new size w*h
    private fun resizeMat(mat: Mat, w: Int, h: Int): Mat {
        val result = Mat()
        matStack.add(result)
        Imgproc.resize(mat, result, Size(w.toDouble(), h.toDouble()))
        return result
    }

    //get a minimal rectangle that wraps the contour
    private fun getMinAreaRect(contour: MatOfPoint): RotatedRect {
        val matOfPoint2f = MatOfPoint2f()
        contour.convertTo(matOfPoint2f, CvType.CV_32F)
        val result = Imgproc.minAreaRect(matOfPoint2f)
        matOfPoint2f.release()
        return result
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
        if (view != null) {
            view!!.first.visibility = f
            view!!.second.visibility = s
            view!!.third.visibility = t
            view!!.last.visibility = l
        }
    }

    fun switchCam(enable: Boolean) {
        enableCamera = enable
        switchCamera()
    }

    override fun onResume() {
        super.onResume()
        reloadSetting()
        if (OpenCVLoader.initDebug()) {
            Log.d("INIT", "OpenCV library found inside package. Using it!");
            initedOpenCV = true
            preloadImage()
            switchCamera()
        } else {
            activity.toast("OpenCV is not found!!")
            activity.finish()
        }
    }

    private fun reloadSetting() {
        isDebug = preferences.getBoolean("is_debug", true)
        detectThreshold = preferences.getString("detect_threshold", "1.5").toDouble()
        cameraSize = preferences.getString("camera_size", "Fullsize")
        signMatchSize = preferences.getInt("sign_size", 100)
        if (view != null) {
            view?.signSizeBar?.max = view?.cameraView?.height!!
            view?.signSizeBar?.progress = signMatchSize
        }
    }
}

