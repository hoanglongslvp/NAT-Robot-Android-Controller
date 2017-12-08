package com.worker.natrobotcontroller.fragments;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.worker.natrobotcontroller.R;
import com.worker.natrobotcontroller.R.id;
import com.worker.natrobotcontroller.activities.MainActivity;

import org.jetbrains.anko.ToastsKt;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import io.github.controlwear.virtual.joystick.android.JoystickView.OnMoveListener;
import kotlin.text.Charsets;

public final class JoystickFragment extends Fragment {

    public String mode = "";
    public int speed = 200;
    public int frontDistance;
    public int backDistance;
    public int currentAngle;
    public int desiredAngle;
    public boolean reading = true;
    TextView info;
    private SharedPreferences preferences;
    private View v;
    private MainActivity activity;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        v = inflater.inflate(R.layout.controller, container, false);
        info = v.findViewById(id.car_info);
        v.findViewById(id.fowardBtn).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                sendCommand("_0;s" + speed + ';');
            }
        });
        v.findViewById(id.backBtn).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                sendCommand("_180;s" + speed + ';');
            }
        });
        v.findViewById(id.leftBtn).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                sendCommand("_90;s" + speed + ';');
            }
        });
        v.findViewById(id.rightBtn).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                sendCommand("_-90;s" + speed + ';');
            }
        });
        v.findViewById(id.stopBtn).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                sendCommand("p10;");
            }
        });
        ((VerticalSeekBar) v.findViewById(id.speedBar)).setProgress(this.speed);
        ((VerticalSeekBar) v.findViewById(id.speedBar)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar p0, int progress, boolean fromUser) {
                speed = progress;
                sendCommand("" + 's' + speed + ';');
            }

            public void onStartTrackingTouch(SeekBar p0) {
            }

            public void onStopTrackingTouch(SeekBar p0) {
            }
        });
        ((JoystickView) v.findViewById(id.joystickR)).setOnMoveListener(new OnMoveListener() {
            public final void onMove(int angle, int strength) {
                int pwm = strength * 255 / 100;
                String cmd = "" + '_' + (angle - 90) + ";s" + pwm + ';';
                sendCommand(cmd);
            }
        }, 200);
        v.findViewById(id.reset_gyro).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View it) {
                sendCommand("g20;");
            }
        });

        ((AppCompatSpinner) v.findViewById(id.controller_mode)).setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onNothingSelected(AdapterView p0) {
            }

            public void onItemSelected(AdapterView p0, View p1, int p2, long p3) {
                String mode = ((AppCompatSpinner) v.findViewById(id.controller_mode)).getSelectedItem().toString();
                selectMode(mode);
                getPreferences().edit().putString("control_mode", mode).apply();
            }
        });
        this.selectMode(this.mode);
        return v;

    }

    public final void startReading() {
        new ReadingTask().execute();
    }

    public final void selectMode(String mode) {
        switch (mode) {
            case "Game pad":
                if (mode.equals("Game pad")) {
                    v.findViewById(id.keyboard_layout).setVisibility(View.VISIBLE);
                    v.findViewById(id.joystickR).setVisibility(View.INVISIBLE);
                }
                break;
            case "Joystick":
                if (mode.equals("Joystick")) {
                    v.findViewById(id.keyboard_layout).setVisibility(View.INVISIBLE);
                    v.findViewById(id.joystickR).setVisibility(View.VISIBLE);
                }
        }

    }

    public final int readNumber(String s) {
        return Integer.parseInt(s.substring((byte) 1, s.length()));
    }

    public void onResume() {
        super.onResume();
        this.reloadSetting();
        this.reading = true;
        this.startReading();
    }

    public void onPause() {
        super.onPause();
        this.reading = false;
    }

    public final void reloadSetting() {
        this.mode = this.getPreferences().getString("control_mode", "Joystick");
    }

    public final void sendCommand(String command) {
        try {
            activity.socket.getOutputStream().write(command.getBytes(Charsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            v.<TextView>findViewById(id.car_info).setText("Not connected");
        }
    }

    public final void slimSendCommand(String command) {
        try {
            activity.socket.getOutputStream().write(command.getBytes(Charsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public final void trigger(int direction) {
        if (direction == -1) {
            this.slimSendCommand("p10;");
        } else {
            int tmp = this.currentAngle + direction * 90;
            while (tmp < 0) {
                tmp += 360;
            }
            while (tmp > 360) {
                tmp -= 360;
            }
            if (tmp < 45) {
                tmp = 0;
            } else if (tmp < 135) {
                tmp = 90;
            } else if (tmp < 225) {
                tmp = 180;
            } else if (tmp < 315) {
                tmp = 270;
            }
            this.slimSendCommand("_" + tmp + ";s250;");
        }

    }

    public SharedPreferences getPreferences() {
        if (preferences == null)
            preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return preferences;
    }

    class ReadingTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            while (reading) {
                try {
                    String line = new BufferedReader(new InputStreamReader(activity.socket.getInputStream())).readLine().trim();
                    if (!line.isEmpty()) {
                        for (String cmd : line.split(";")) {
                            if (!cmd.isEmpty()) {
                                switch (cmd.charAt(0)) {
                                    case 'b':
                                        backDistance = readNumber(cmd);
                                        break;
                                    case 'c':
                                        currentAngle = readNumber(cmd);
                                        break;
                                    case 'd':
                                        desiredAngle = readNumber(cmd);
                                    case 'e':
                                    default:
                                        break;
                                    case 'f':
                                        frontDistance = readNumber(cmd);
                                }
                            }
                        }
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        public final void run() {
                            info.setText(String.format("Front: %d cm\nBack: %d\nCurrent angle: %d\nDesired angle: %d", frontDistance, backDistance, currentAngle, desiredAngle));
                        }
                    });
                } catch (Exception e) {
                    if (e instanceof NullPointerException)
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    else
                        e.printStackTrace();
                }
            }
            return null;
        }
    }

}
