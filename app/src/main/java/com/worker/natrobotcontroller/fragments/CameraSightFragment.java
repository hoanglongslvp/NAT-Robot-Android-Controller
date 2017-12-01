package com.worker.natrobotcontroller.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.worker.natrobotcontroller.R;
import com.worker.natrobotcontroller.R.id;
import com.worker.natrobotcontroller.activities.MainActivity;
import com.worker.natrobotcontroller.models.MatchSignResult;
import com.worker.natrobotcontroller.models.TrafficSign;
import com.worker.natrobotcontroller.util.MatUtil;

import org.jetbrains.anko.ToastsKt;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class CameraSightFragment extends Fragment {
    private boolean enableCamera;
    private boolean initedOpenCV;
    private boolean isDebug;

    private String cameraSize = "Fullsize";
    private double detectThreshold = 1.5D;

    private Scalar detectColor = new Scalar(25.0D, 118.0D, 210.0D);

    private Scalar selectColor = new Scalar(255.0D, 64.0D, 129.0D);

    private List<Mat> matStack = new ArrayList<>();

    private List<TrafficSign> signs = new ArrayList<>();

    private MatchSignResult bestResult;
    private int signMatchSize;
    private SharedPreferences preferences;


    public SharedPreferences getPreferences() {
        if (preferences == null)
            preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return preferences;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.camera_sight, container, false);
        this.setupView(v);
        return v;
    }

    private void setupView(View v) {
        ((JavaCameraView) v.findViewById(id.cameraView)).setCvCameraViewListener(new CvCameraViewListener2() {
            public void onCameraViewStarted(int width, int height) {
            }

            public void onCameraViewStopped() {
            }


            public Mat onCameraFrame(CvCameraViewFrame frame) {
                for (Mat m : matStack) {
                    m.release();
                }
                matStack.clear();
                Mat rgba = frame.rgba();
                matStack.add(rgba);
                Mat mask = getColorMask(rgba);
                List<MatOfPoint> contours = getContours(mask);
                bestResult = null;
                if (contours.size() > 0) {
                    if (isDebug) {
                        Imgproc.drawContours(rgba, contours, -1, detectColor);
                    }

                    for (MatOfPoint contour : contours) {
                        RotatedRect rect = getMinAreaRect(contour);
                        if (rect.size.height > (double) signMatchSize) {
                            if (isDebug) {
                                MatUtil.draw(rect, rgba, detectColor);
                            }

                            MatUtil.fixRotation(rect);
                            Mat rotationMatrix2D = Imgproc.getRotationMatrix2D(rect.center, rect.angle, 1.0D);
                            matStack.add(rotationMatrix2D);
                            Mat thumbnail = resizeMat(getThumbnail(mask, rect, rotationMatrix2D), 50, 50);
                            MatchSignResult best = getBestMatchSign(thumbnail, rect);
                            if (best != null) {
                                if (bestResult == null) {
                                    bestResult = best;
                                } else {
                                    if (best.getRect().size.area() > bestResult.getRect().size.area()) {
                                        bestResult = best;
                                    }
                                }

                                if (isDebug) {
                                    Imgproc.putText(rgba, best.getSign().msg + ":" + best.getError(), bestResult.getRect().center, 0, 0.65D, detectColor, 2);
                                }
                            }
                        }
                        contour.release();
                    }

                    if (bestResult != null) {
                        MatUtil.draw(bestResult.getRect(), rgba, selectColor);
                        trigger(bestResult.getSign().direction);
                        Imgproc.putText(rgba, bestResult.getSign().msg, bestResult.getRect().center, 0, 0.65D, selectColor, 2);
                        if (isDebug) {
                            Mat cropped = new Mat();
                            matStack.add(cropped);
                            Imgproc.cvtColor(bestResult.getThumbnail(), cropped, Imgproc.COLOR_GRAY2RGBA);
                            cropped.copyTo(new Mat(rgba, new Rect(20, 20, cropped.width(), cropped.height())));
                        }
                    }
                }

                drawSignSizeIndicator(rgba);
                return rgba;
            }
        });
        ((VerticalSeekBar) v.findViewById(id.signSizeBar)).setMax(v.findViewById(id.cameraView).getHeight());
        ((VerticalSeekBar) v.findViewById(id.signSizeBar)).setProgress(this.signMatchSize);
        ((VerticalSeekBar) v.findViewById(id.signSizeBar)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar p0, int progress, boolean isUser) {
                signMatchSize = progress;
            }

            public void onStartTrackingTouch(SeekBar p0) {
            }

            public void onStopTrackingTouch(SeekBar p0) {
                getPreferences().edit().putInt("sign_size", signMatchSize).apply();
            }
        });
    }

    private void trigger(int direction) {
        ((MainActivity) this.getActivity()).controller.trigger(direction);
    }

    private void drawSignSizeIndicator(Mat rgba) {
        Imgproc.line(rgba, new Point((double) rgba.width(), 10.0D), new Point((double) rgba.width() - (double) 20, 10.0D), this.selectColor, 2);
        Imgproc.line(rgba, new Point((double) rgba.width(), 10.0D + (double) this.signMatchSize), new Point((double) rgba.width() - (double) 20, 10.0D + (double) this.signMatchSize), this.selectColor, 2);
    }

    private MatchSignResult getBestMatchSign(Mat thumbnail, RotatedRect rect) {
        TrafficSign sign = null;
        double diff = 1.0E8D;
        for (TrafficSign s : signs) {
            double d = measureDifferenceBetween(s.img, thumbnail);
            if (d < diff) {
                sign = s;
                diff = d;
            }
        }
        if (sign != null && diff < detectThreshold) {
            return new MatchSignResult(sign, diff, thumbnail, rect);
        } else
            return null;
    }

    private Mat getThumbnail(Mat mask, RotatedRect rect, Mat rotationMatrix2D) {
        Mat rotatedMask = new Mat();
        this.matStack.add(rotatedMask);
        Imgproc.warpAffine(mask, rotatedMask, rotationMatrix2D, mask.size(), Imgproc.INTER_CUBIC);
        Mat cropped = new Mat();
        this.matStack.add(cropped);
        Imgproc.getRectSubPix(rotatedMask, rect.size, rect.center, cropped);
        Core.bitwise_not(cropped, cropped);
        return cropped;
    }

    private void preloadImage() {
        signs.add(new TrafficSign("Turn right", getActivity(), R.drawable.turnright, TrafficSign.RIGHT));
        signs.add(new TrafficSign("Turn left", getActivity(), R.drawable.turnleft, TrafficSign.LEFT));
        signs.add(new TrafficSign("Turn back", getActivity(), R.drawable.turnback, TrafficSign.BACK));
        signs.add(new TrafficSign("Move straight", getActivity(), R.drawable.movestraight, TrafficSign.STRAIGHT));
        signs.add(new TrafficSign("Stop", getActivity(), R.drawable.stopx, TrafficSign.STOP));
    }

    private double measureDifferenceBetween(Mat A, Mat B) {
        if (A.rows() > 0 && A.rows() == B.rows() && A.cols() > 0 && A.cols() == B.cols()) {
            double errorL2 = Core.norm(A, B, Core.NORM_L2);
            double error = errorL2 / (double) (A.rows() * A.cols());
            return error;
        } else {
            return 1.0E8D;
        }
    }

    private Mat resizeMat(Mat mat, int w, int h) {
        Mat result = new Mat();
        this.matStack.add(result);
        Imgproc.resize(mat, result, new Size((double) w, (double) h));
        return result;
    }

    private RotatedRect getMinAreaRect(MatOfPoint contour) {
        MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        contour.convertTo(matOfPoint2f, CvType.CV_32F);
        RotatedRect result = Imgproc.minAreaRect(matOfPoint2f);
        matOfPoint2f.release();
        return result;
    }

    private List<MatOfPoint> getContours(Mat mask) {
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        this.matStack.add(hierarchy);
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0.0D, 0.0D));
        return contours;
    }

    private Mat getColorMask(Mat rgba) {
        Mat hsv = new Mat();
        this.matStack.add(hsv);
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV, 3);
        Mat mask = new Mat();
        this.matStack.add(mask);
        Scalar lower_color = new Scalar(80.0D, 100.0D, 70.0D);
        Scalar upper_color = new Scalar(138.5D, 255.0D, 255.0D);
        Core.inRange(hsv, lower_color, upper_color, mask);
        return mask;
    }

    private void switchCamera() {
        if (this.initedOpenCV) {
            JavaCameraView cam = this.getView().findViewById(id.cameraView);
            if (this.enableCamera) {
                this.reloadSetting();
                cam.enableView();
            } else {
                cam.disableView();
            }
        }

        switch (cameraSize) {
            case "Medium":
                this.setSize(View.INVISIBLE, View.VISIBLE, View.INVISIBLE, View.VISIBLE);
                break;
            case "Small":
                this.setSize(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE);
                break;
            case "Fullsize":
                this.setSize(View.VISIBLE, View.VISIBLE, View.VISIBLE, View.VISIBLE);
        }

    }

    private void setSize(int first, int second, int third, int last) {
        if (this.getView() != null) {
            this.getView().findViewById(id.first).setVisibility(first);
            this.getView().findViewById(id.second).setVisibility(second);
            this.getView().findViewById(id.third).setVisibility(third);
            this.getView().findViewById(id.last).setVisibility(last);
        }

    }

    public void switchCam(boolean enable) {
        this.enableCamera = enable;
        this.switchCamera();
    }

    public void onResume() {
        super.onResume();
        this.reloadSetting();
        if (!this.initedOpenCV) {
            if (OpenCVLoader.initDebug()) {
                Log.d("INIT", "OpenCV library found inside package. Using it!");
                this.initedOpenCV = true;
                this.preloadImage();
                this.switchCamera();
            } else {
                ToastsKt.longToast(this.getActivity(), "OpenCV is not found!!");
                this.getActivity().finish();
            }
        }

    }

    private void reloadSetting() {
        this.isDebug = this.getPreferences().getBoolean("is_debug", true);
        this.detectThreshold = Double.parseDouble(this.getPreferences().getString("detect_threshold", "1.5"));
        this.cameraSize = this.getPreferences().getString("camera_size", "Fullsize");
        this.signMatchSize = this.getPreferences().getInt("sign_size", 100);
        if (getView() != null) {
            VerticalSeekBar seek = getView().findViewById(id.signSizeBar);
            seek.setMax(getView().findViewById(id.cameraView).getHeight());
            seek.setProgress(signMatchSize);
        }
    }

}
