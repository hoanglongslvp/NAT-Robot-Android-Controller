package com.worker.natrobotcontroller.components;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.widget.AppCompatSpinner;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import com.worker.natrobotcontroller.R.id;
import com.worker.natrobotcontroller.activities.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import io.github.controlwear.virtual.joystick.android.JoystickView.OnMoveListener;
import kotlin.text.Charsets;

public class JoystickFragment {

    public int backDistance;
    public int currentAngle;
    public int desiredAngle;
    public boolean reading = true;
    private TextView info;
    private String mode = "";
    private int speed = 200;
    private int frontDistance;
    private SharedPreferences preferences;
    private View view;
    private MainActivity activity;

    public JoystickFragment(MainActivity activity, View root) {
        this.activity = activity;
        view = root;
        info = view.findViewById(id.car_info);
        view.findViewById(id.fowardBtn).setOnClickListener(new OnClickListener() {
            public void onClick(View it) {
                sendCommand("_0;s" + speed + ';');
            }
        });
        view.findViewById(id.backBtn).setOnClickListener(new OnClickListener() {
            public void onClick(View it) {
                sendCommand("_180;s" + speed + ';');
            }
        });
        view.findViewById(id.leftBtn).setOnClickListener(new OnClickListener() {
            public void onClick(View it) {
                sendCommand("_90;s" + speed + ';');
            }
        });
        view.findViewById(id.rightBtn).setOnClickListener(new OnClickListener() {
            public void onClick(View it) {
                sendCommand("_-90;s" + speed + ';');
            }
        });
        view.findViewById(id.stopBtn).setOnClickListener(new OnClickListener() {
            public void onClick(View it) {
                sendCommand("p10;");
            }
        });
        ((VerticalSeekBar) view.findViewById(id.speedBar)).setProgress(this.speed);
        ((VerticalSeekBar) view.findViewById(id.speedBar)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar p0, int progress, boolean fromUser) {
                speed = progress;
                sendCommand("" + 's' + speed + ';');
            }

            public void onStartTrackingTouch(SeekBar p0) {
            }

            public void onStopTrackingTouch(SeekBar p0) {
            }
        });
        ((JoystickView) view.findViewById(id.joystickR)).setOnMoveListener(new OnMoveListener() {
            public void onMove(int angle, int strength) {
                int pwm = strength * 255 / 100;
                String cmd = "" + '_' + (angle - 90) + ";s" + pwm + ';';
                sendCommand(cmd);
            }
        }, 200);
        view.findViewById(id.reset_gyro).setOnClickListener(new View.OnClickListener() {
            public void onClick(View it) {
                sendCommand("g20;");
            }
        });

        ((AppCompatSpinner) view.findViewById(id.controller_mode)).setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onNothingSelected(AdapterView p0) {
            }

            public void onItemSelected(AdapterView p0, View p1, int p2, long p3) {
                String mode = ((AppCompatSpinner) view.findViewById(id.controller_mode)).getSelectedItem().toString();
                selectMode(mode);
                getPreferences().edit().putString("control_mode", mode).apply();
            }
        });
        this.selectMode(this.mode);

    }

    public void startReading() {
        this.reading = true;
        new ReadingTask().execute();
    }

    public void selectMode(String mode) {
        switch (mode) {
            case "Game pad":
                if (mode.equals("Game pad")) {
                    view.findViewById(id.keyboard_layout).setVisibility(View.VISIBLE);
                    view.findViewById(id.joystickR).setVisibility(View.INVISIBLE);
                }
                break;
            case "Joystick":
                if (mode.equals("Joystick")) {
                    view.findViewById(id.keyboard_layout).setVisibility(View.INVISIBLE);
                    view.findViewById(id.joystickR).setVisibility(View.VISIBLE);
                }
        }

    }

    private int readNumber(String s) {
        return Integer.parseInt(s.substring((byte) 1, s.length()));
    }


    private void sendCommand(String command) {
        if (activity.socket == null) {
            activity.setOffline();
        } else
            try {
                activity.socket.getOutputStream().write(command.getBytes(Charsets.UTF_8));
                activity.setOnline();
            } catch (Exception e) {
                e.printStackTrace();
                view.<TextView>findViewById(id.car_info).setText("Not connected");
                activity.setOffline();
            }
    }

    private void slimSendCommand(String command) {
        if (activity.socket == null) {
            activity.setOffline();
        } else
            try {
                activity.socket.getOutputStream().write(command.getBytes(Charsets.UTF_8));
                activity.setOnline();
            } catch (Exception e) {
                e.printStackTrace();
                activity.setOffline();
            }
    }

    public void trigger(int direction) {
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
            preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        return preferences;
    }

    public void show() {
        view.setVisibility(View.VISIBLE);
    }

    public void hide() {
        view.setVisibility(View.INVISIBLE);
    }

    public void stopReading() {
        this.reading = false;
    }

    private void delay() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }

    private class ReadingTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            while (reading) {
                if (activity.socket == null) {
                    delay();
                    activity.setOffline();
                } else
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
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                info.setText(String.format("Front: %d cm\nBack: %d\nCurrent angle: %d\nDesired angle: %d", frontDistance, backDistance, currentAngle, desiredAngle));
                                activity.setOnline();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        activity.setOffline();
                        delay();
                    }
            }
            return null;
        }
    }

}
