package com.worker.natrobotcontroller.components;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;

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

public class CameraSightFragment {
    public static final int MIN_SIZE = 1;
    public static final int BAD_NUMBER = 100000000;
    public static final double INDICATOR_LENGHT = 10;
    private View view;
    private boolean initedOpenCV;
    private boolean isDebug;
    private double detectThreshold = 1.5D;
    private Scalar LIGHT_BLUE = new Scalar(25.0D, 118.0D, 210);
    private Scalar RED = new Scalar(255.0D, 64.0D, 129.0D);
    private List<Mat> matStack = new ArrayList<>();
    private List<TrafficSign> signs = new ArrayList<>();
    private MatchSignResult bestResult;
    private int signMatchSize;
    private SharedPreferences preferences;
    private JavaCameraView cameraView;
    private JoystickFragment controller;
    private SeekBar signSeekBar;
    private float cameraScaledRatio = 1;
    private CheckBox isDebugCB;
    private EditText thresholdED;
    private Spinner screenSizeSpinner;
    private MainActivity activity;
    private int maxWidth = -1;
    private int oldSize;
    private boolean isShow = false;

    public CameraSightFragment(MainActivity activity, View root) {
        this.activity = activity;
        this.view = root;
        controller = this.activity.controller;
        initOpenCV();
        setupView(root);
    }

    private SharedPreferences getPreferences() {
        if (preferences == null)
            preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        return preferences;
    }

    private void setupView(View v) {
        cameraView = v.findViewById(id.cameraView);
        cameraView.setCvCameraViewListener(new CvCameraViewListener2() {
            public void onCameraViewStarted(int width, int height) {
                if (maxWidth == -1) {
                    maxWidth = ((FrameLayout) cameraView.getParent()).getWidth();
                }
                cameraScaledRatio = (16f / 9) / (1f * width / height);
                signSeekBar.setMax(height - 10);
                signMatchSize = signSeekBar.getMax() / 2;
                signSeekBar.setProgress(signMatchSize);
            }

            public void onCameraViewStopped() {
            }

            public Mat onCameraFrame(CvCameraViewFrame frame) {
                if (isShow) {
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
                            Imgproc.drawContours(rgba, contours, -1, LIGHT_BLUE);
                        }

                        for (MatOfPoint contour : contours) {
                            RotatedRect rect = getMinAreaRect(contour);
                            if ((rect.size.height > (double) signMatchSize) && isPrettySquare(rect.size)) {

                                if (isDebug) {
                                    MatUtil.draw(rect, rgba, LIGHT_BLUE);
                                }
                                MatUtil.fixRotation(rect);
                                Mat rotationMatrix2D = Imgproc.getRotationMatrix2D(rect.center, rect.angle, 1.0D);
                                matStack.add(rotationMatrix2D);
                                Mat thumbnail = resizeMat(getThumbnail(mask, rect, rotationMatrix2D), 50, 50);
                                thumbnail = MatUtil.toBinaryMat(thumbnail);
                                matStack.add(thumbnail);
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
                                        Imgproc.putText(rgba, best.getSign().msg + ":" + best.getError(), bestResult.getRect().center, 0, 0.65D, LIGHT_BLUE, 2);
                                    }
                                }
                            }
                            contour.release();
                        }

                        if (bestResult != null) {
                            MatUtil.draw(bestResult.getRect(), rgba, RED);
                            if (isShow)
                                controller.trigger(bestResult.getSign().direction);
                            Imgproc.putText(rgba, bestResult.getSign().msg, bestResult.getRect().center, 0, 0.65D, RED, 2);
                            if (isDebug) {
                                Mat cropped = new Mat();
                                matStack.add(cropped);
                                Imgproc.cvtColor(bestResult.getThumbnail(), cropped, Imgproc.COLOR_GRAY2RGBA);
                                Mat dest = new Mat(rgba, new Rect(20, 20, cropped.width(), cropped.height()));
                                matStack.add(dest);
                                cropped.copyTo(dest);
                            }
                        }
                    }

                    drawSignSizeIndicator(rgba);
                    return rgba;
                }
                return null;
            }
        });

        signSeekBar = v.findViewById(R.id.signSizeBar);
        signSeekBar.setMax(cameraView.getHeight());
        signSeekBar.setProgress(this.signMatchSize);
        signSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean isUser) {
                if (isUser)
                    if (progress >= MIN_SIZE)
                        signMatchSize = progress;
                    else seekBar.setProgress(MIN_SIZE);
            }

            public void onStartTrackingTouch(SeekBar p0) {
            }

            public void onStopTrackingTouch(SeekBar p0) {
                if (signMatchSize >= MIN_SIZE)
                    getPreferences().edit().putInt("sign_size", signMatchSize).apply();
            }
        });
        isDebugCB = v.findViewById(id.isDebugCB);
        isDebugCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isDebug = b;
            }
        });
        thresholdED = v.findViewById(id.thresholdED);
        thresholdED.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    detectThreshold = Float.parseFloat(editable.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastsKt.toast(activity, "Threshold value is malformed!");
                }
            }
        });
        screenSizeSpinner = v.findViewById(id.screenSizeSpinner);
        screenSizeSpinner.setSelection(0, false);
        screenSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setCameraSize(i);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        cameraView.enableView();
    }

    private void setCameraSize(int size) {
        if (size != oldSize) {
            activity.log("Camera is changing size, please wait...");
            ViewGroup.LayoutParams layoutParams = cameraView.getLayoutParams();
            layoutParams.width = maxWidth / (size + 1);
            cameraView.setLayoutParams(layoutParams);
            oldSize = size;
        }
        cameraView.enableView();
    }

    private boolean isPrettySquare(Size size) {
        double fixedWidth = size.width * cameraScaledRatio;
        if (fixedWidth > size.height)
            return fixedWidth / size.height < 1.2;
        else return fixedWidth / size.height > 0.8;
    }

    private void drawSignSizeIndicator(Mat rgba) {
        Imgproc.line(rgba, new Point((double) rgba.width(), INDICATOR_LENGHT), new Point((double) rgba.width() - (double) 20, INDICATOR_LENGHT), this.RED, 2);
        Imgproc.line(rgba, new Point((double) rgba.width(), INDICATOR_LENGHT + (double) this.signMatchSize), new Point((double) rgba.width() - (double) 20, INDICATOR_LENGHT + (double) this.signMatchSize), this.RED, 2);
    }

    private MatchSignResult getBestMatchSign(Mat thumbnail, RotatedRect rect) {
        TrafficSign sign = null;
        double diff = BAD_NUMBER;
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
        signs.add(new TrafficSign("Turn right", activity, R.drawable.turnright, TrafficSign.RIGHT));
        signs.add(new TrafficSign("Turn left", activity, R.drawable.turnleft, TrafficSign.LEFT));
        signs.add(new TrafficSign("Turn back", activity, R.drawable.turnback, TrafficSign.BACK));
        signs.add(new TrafficSign("Move straight", activity, R.drawable.movestraight, TrafficSign.STRAIGHT));
        signs.add(new TrafficSign("Stop", activity, R.drawable.stopx, TrafficSign.STOP));
    }

    private double measureDifferenceBetween(Mat A, Mat B) {
        if (A.rows() > 0 && A.rows() == B.rows() && A.cols() > 0 && A.cols() == B.cols()) {
            double errorL2 = Core.norm(A, B, Core.NORM_L2);
            return errorL2 / (double) (A.rows() * A.cols());
        } else {
            return BAD_NUMBER;
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
        matStack.add(matOfPoint2f);
        contour.convertTo(matOfPoint2f, CvType.CV_32F);
        RotatedRect result = Imgproc.minAreaRect(matOfPoint2f);
        matOfPoint2f.release();
        return result;
    }

    private List<MatOfPoint> getContours(Mat mask) {
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        this.matStack.add(hierarchy);
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0.0D, 0.0D));
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

    public void hide() {
        view.setVisibility(View.INVISIBLE);
        isShow = false;
    }

    public void show() {
        isShow = true;
        view.setVisibility(View.VISIBLE);
        if (!this.initedOpenCV)
            initOpenCV();
    }

    private void initOpenCV() {
        if (OpenCVLoader.initDebug()) {
            Log.d("INIT", "OpenCV library found inside package. Using it!");
            this.initedOpenCV = true;
            this.preloadImage();
        } else {
            activity.log("OpenCV is not found!!");
            this.activity.finish();
        }
    }
}
