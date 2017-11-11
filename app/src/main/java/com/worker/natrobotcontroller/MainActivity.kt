package com.worker.natrobotcontroller

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SwitchCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import com.github.kayvannj.permission_utils.Func
import com.github.kayvannj.permission_utils.PermissionUtil
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.core.Core.FONT_HERSHEY_SIMPLEX
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*


class MainActivity : AppCompatActivity() {
    var enableCamera = true
    var initedOpenCV = false
    var firstRun = true
    val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    val signs = mutableListOf<TrafficSignImage>()
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        title = item.title
        when (item.itemId) {
            R.id.navigation_connect -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_camera -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_control -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        startActivity(Intent(this, SettingActivity::class.java))
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //don't let the screen turn off
        setContentView(R.layout.activity_main)
        askForPermission()
    }

    private fun preloadImage() {
        signs.add(TrafficSignImage("Turn right", this, R.drawable.turnright))
        signs.add(TrafficSignImage("Turn left", this, R.drawable.turnleft))
        signs.add(TrafficSignImage("Turn back", this, R.drawable.turnback))
        signs.add(TrafficSignImage("Move straight", this, R.drawable.movestraight))
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

        override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
        }

    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d("INIT", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, openCVInitCallback);
        } else {
            Log.d("INIT", "OpenCV library found inside package. Using it!");
            openCVInitCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    private fun setupUI() {
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        cameraView.setCvCameraViewListener(object : CameraBridgeViewBase.CvCameraViewListener2 {
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
                    return rgba
                }
                contours.forEach { it.release() }
                mask.release()
                System.gc()
                System.runFinalization()
                return result as Mat
            }

        })
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
        warpAffine(rgba, rotatedMat, rotationMatrix2D, rgba.size(), INTER_CUBIC)
        val cropped = Mat()

        val rotatedChannels = mutableListOf<Mat>()
        val croppedChannels = mutableListOf<Mat>()
        val tempChannel = Mat()
        Core.split(rotatedMat, rotatedChannels)
        for (channel in rotatedChannels) {
            getRectSubPix(channel, rect.size, rect.center, tempChannel)
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
        warpAffine(mask, rotatedMat, rotationMatrix2D, rgba.size(), INTER_CUBIC)
        var cropped = Mat()
        getRectSubPix(rotatedMat, rect.size, rect.center, cropped)
        Core.bitwise_not(cropped, cropped)
        var rgbaCropped = Mat()
        cvtColor(cropped, rgbaCropped, COLOR_GRAY2RGBA)
        rgbaCropped = resizeMat(rgbaCropped, 50, 50)
        cropped = resizeMat(cropped, 50, 50)
        rgbaCropped.copyTo(Mat(rgba, Rect(20, 20, rgbaCropped.width(), rgbaCropped.height())))
        var sign: TrafficSignImage? = null
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
                putText(rgba, "Match : " + sign.msg + " : " + similary, Point(20.0, 100.0), FONT_HERSHEY_SIMPLEX, 0.65, Scalar(0.0, 255.0, 0.0), 2)
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
        resize(mat, result, Size(w.toDouble(), h.toDouble()))
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
            line(mat, vertices[j], vertices[(j + 1) % 4], Scalar(0.0, 255.0, 0.0))
        }
    }

    private fun askForPermission() {
        PermissionUtil.with(this).request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE).onAllGranted(object : Func() {
            override fun call() {
                setupUI()
            }
        }
        ).ask(134) //134 is just a random number
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.switch_menu, menu)
        val switch = menu.findItem(R.id.camera_switcher).actionView as SwitchCompat
//        switch.isChecked = preferences.getBoolean(getString(R.string.setting_camera_on), false)
        switch.setOnCheckedChangeListener { button, checked ->
            enableCamera = checked
            switchCamera()
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.setting_action -> startActivity(Intent(this, SettingActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun switchCamera() {
        if (initedOpenCV) //must be sure that openCV has been initialized before enabling Camera
            if (enableCamera)
                cameraView.enableView()
            else
                cameraView.disableView()
    }

}
