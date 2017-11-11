package com.worker.natrobotcontroller

import android.Manifest
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SwitchCompat
import android.util.Log
import android.view.Menu
import android.view.WindowManager
import com.github.kayvannj.permission_utils.Func
import com.github.kayvannj.permission_utils.PermissionUtil
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*


class MainActivity : AppCompatActivity() {
    var enableCamera = true
    var initedOpenCV = false

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        title = item.title
        when (item.itemId) {
            R.id.navigation_home -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main)
        askForPermission()

    }

    override fun onResume() {
        super.onResume()
        val callback: LoaderCallbackInterface = object : LoaderCallbackInterface {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.i("Opencv", "OpenCV loaded successfully")
                        initedOpenCV = true
                        switchCamera()
                    }
                    else -> {
                        Log.w("Opencv", "OpenCV loaded fails")
                    }
                }
            }

            override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
            }

        }
        if (!OpenCVLoader.initDebug()) {
            Log.d("INIT", "Internal OpenCV library not found. Using OpenCV Manager for initialization");

            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, callback);
        } else {
            Log.d("INIT", "OpenCV library found inside package. Using it!");
            callback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private fun setupUI() {
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        cameraView.setCvCameraViewListener(object : CameraBridgeViewBase.CvCameraViewListener2 {
            var result: Mat? = null
            override fun onCameraViewStarted(width: Int, height: Int) {

            }

            override fun onCameraViewStopped() {
            }

            override fun onCameraFrame(frame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
//                return frame.rgba()
                result?.release()
                val rgba = frame.rgba()
                result = rgba
                val mask = getBlueMask(rgba)
                val contours = getContour(mask)
                val contour_index = getBiggestContour(contours)

                if (contour_index != -1) {
                    Imgproc.drawContours(rgba, contours, contour_index,
                            Scalar(Math.random() * 255, Math.random() * 255, Math.random() * 255))
                    val contour = contours[contour_index]
                    val matOfPoint2f = MatOfPoint2f()
                    contour.convertTo(matOfPoint2f, CvType.CV_32F)
                    val rect = Imgproc.minAreaRect(matOfPoint2f)
                    draw(rect, rgba)

                    var angle = rect.angle
                    val rect_size = rect.size
                    if (angle < -45.0) {
                        angle += 90.0
                        val tmp = rect_size.width
                        rect_size.width = rect_size.height
                        rect_size.height = tmp
                    }
                    val rotationMatrix2D = Imgproc.getRotationMatrix2D(rect.center, angle, 1.0)
                    val rotated = Mat()
                    Imgproc.warpAffine(rgba, rotated, rotationMatrix2D, rgba.size(), INTER_CUBIC)
                    val cropped = Mat()

                    val channels = mutableListOf<Mat>()
                    val tmp = mutableListOf<Mat>()
                    val t = Mat()
                    Core.split(rotated, channels)
                    for (channel in channels) {
                        getRectSubPix(channel, rect_size, rect.center, t)
                        tmp.add(t.clone())
                    }
                    Core.merge(tmp, cropped)
                    cropped.copyTo(Mat(rgba, Rect(20, 20, cropped.width(), cropped.height())))
                    channels.forEach { it.release() }
                    tmp.forEach { it.release() }
                    matOfPoint2f.release()
                    rotated.release()
                    cropped.release()
                    return rgba
                }
                mask.release()
                return result as Mat
            }

        })
    }

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

    private fun getContour(mask: Mat): MutableList<MatOfPoint> {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, Point(0.0, 0.0))
        hierarchy.release()
        return contours
    }

    private fun getBlueMask(rgba: Mat): Mat {
        val hsv = Mat()
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV, 3)
        val mask = Mat()
        val lower_color = Scalar(160.0 / 2, 100.0, 70.0)
        val upper_color = Scalar(277.0 / 2, 255.0, 255.0)
        Core.inRange(hsv, lower_color, upper_color, mask)
        hsv.release()
        return mask
    }

    private fun draw(rRect: RotatedRect, mat: Mat) {
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
        ).ask(134)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.switch_menu, menu)
        (menu.findItem(R.id.camera_switcher).actionView as SwitchCompat).setOnCheckedChangeListener { button, checked ->
            enableCamera = checked
            switchCamera()
        }
        return super.onCreateOptionsMenu(menu)
    }

    private fun switchCamera() {
        if (initedOpenCV)
            if (enableCamera)
                cameraView.enableView()
            else
                cameraView.disableView()
    }

}
