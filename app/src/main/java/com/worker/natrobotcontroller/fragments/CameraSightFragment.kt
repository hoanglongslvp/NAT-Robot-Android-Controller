package com.worker.natrobotcontroller.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.SwitchCompat
import android.util.Log
import android.view.*
import com.worker.natrobotcontroller.R
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
    val signs = mutableListOf<TrafficSign>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.camera_sight, container, false)
        setupView(v)
        return v
    }

    private fun setupView(v:View) {
        v.cameraView.setCvCameraViewListener(object : CameraBridgeViewBase.CvCameraViewListener2 {
            var result: Mat? = null
            override fun onCameraViewStarted(width: Int, height: Int) {}

            override fun onCameraViewStopped() {}

            override fun onCameraFrame(frame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
                result?.release()
                val rgba = frame.rgba()
                result = rgba
                val mask = getColorMask(rgba)
                val contours = getContour(mask)
                val contour_index = getBiggestContour(contours)

                if (contour_index != -1) { //we got a sign
                    val contour = contours[contour_index] //the biggest contour
                    val randomColor = Scalar(Math.random() * 255, Math.random() * 255, Math.random() * 255)
                    Imgproc.drawContours(rgba, contours, contour_index,
                            randomColor)
                    val rect = getMinAreaRect(contour)
                    if (rect.size.width > 10 && rect.size.height > 10) {
                        drawRotatedRect(rect, rgba)
                        fixRotationRect(rect)

                        //get rotation matrix from the rotated rectangle
                        val rotationMatrix2D = Imgproc.getRotationMatrix2D(rect.center, rect.angle, 1.0)

//                    drawThumbnail(rgba, rotationMatrix2D, rect)
                        drawMaskThumbnail(rgba, rotationMatrix2D, rect, mask)
                        rotationMatrix2D.release()
                    }
                }
                contours.forEach { it.release() }
                mask.release()
//                System.gc()
//                System.runFinalization()
                return result as Mat
            }

        })
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
        Log.d("Camerasight", "Resume")
        if (!OpenCVLoader.initDebug()) {
            Log.d("INIT", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, activity, openCVInitCallback);
        } else {
            Log.d("INIT", "OpenCV library found inside package. Using it!");
            openCVInitCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
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
    private fun drawThumbnail(rgba: Mat, rotationMatrix2D: Mat?, rect: RotatedRect) {
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
            val simi = getSimilarity(s.img, cropped)
            if (simi < similary) {
                sign = s
                similary = simi
            }
            Log.d("Similary", s.msg + " : " + simi)
        }

        Log.d("Similary", "Best " + (sign?.msg))
        if (sign != null) {
            if (similary < 1.5)
                Imgproc.putText(rgba, "Match : " + sign.msg + " : " + similary, Point(20.0, 100.0), Core.FONT_HERSHEY_SIMPLEX, 0.65, Scalar(0.0, 255.0, 0.0), 2)
        }
        rgbaCropped.release()
        rotatedMat.release()
        cropped.release()
    }

    fun getSimilarity(A: Mat, B: Mat): Double {
        if (A.rows() > 0 && A.rows() == B.rows() && A.cols() > 0 && A.cols() == B.cols()) {
            // Calculate the L2 relative error between images.
            val errorL2 = Core.norm(A, B, Core.NORM_L2);
            // Convert to a reasonable scale, since L2 error is summed across all pixels of the image.
            val similarity = errorL2 / (A.rows() * A.cols());
            return similarity;
        } else {
            //Images have a different size
            return 100000000.0;  // Return a bad value
        }
    }

    private fun resizeMat(mat: Mat, w: Int, h: Int): Mat {
        val result = Mat()
        Imgproc.resize(mat, result, Size(w.toDouble(), h.toDouble()))
        mat.release()
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
    private fun getContour(mask: Mat): MutableList<MatOfPoint> {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, Point(0.0, 0.0))
        hierarchy.release()
        return contours
    }

    //generate a binary image from a full color image
    private fun getColorMask(rgba: Mat): Mat {
        val hsv = Mat()
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV, 3) //convert rgb image to hsv image
        val mask = Mat()
        val lower_color = Scalar(160.0 / 2, 100.0, 70.0) //the lower color limit
        val upper_color = Scalar(277.0 / 2, 255.0, 255.0) //the upper color limit
        Core.inRange(hsv, lower_color, upper_color, mask) //get all a pixel that stays between two limits
        hsv.release() //don't forget to release any Mat (Image)
        return mask
    }

    private fun drawRotatedRect(rRect: RotatedRect, mat: Mat) {
        val vertices = arrayOfNulls<Point>(4)
        rRect.points(vertices)
        for (j in 0..3) {
            Imgproc.line(mat, vertices[j], vertices[(j + 1) % 4], Scalar(0.0, 255.0, 0.0))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.switch_menu, menu)
        val switch = menu.findItem(R.id.camera_switcher).actionView as SwitchCompat
        switch.setOnCheckedChangeListener { button, checked ->
            enableCamera = checked
            switchCamera()
        }
        super.onCreateOptionsMenu(menu, inflater)
    }





    private fun switchCamera() {
        if (initedOpenCV) //must be sure that openCV has been initialized before enabling Camera
            if (enableCamera)
                view?.cameraView?.enableView()
            else
                view?.cameraView?.disableView()
    }

    fun switchCam(enable: Boolean) {
        enableCamera = enable
        Log.d("switchcam","called with "+enable)
        switchCamera()
    }
}