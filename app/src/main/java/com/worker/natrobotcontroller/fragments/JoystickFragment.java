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
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import io.github.controlwear.virtual.joystick.android.JoystickView.OnMoveListener;
import kotlin.text.Charsets;

public final class JoystickFragment extends Fragment {
    @NotNull
    public String mode = "";
    public int speed = 200;
    public int frontDistance;
    public int backDistance;
    public int currentAngle;
    public int desiredAngle;
    @NotNull
    public ConnectFragment connect;
    public boolean reading = true;
    private SharedPreferences preferences;


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.connect = ((MainActivity) getActivity()).connect;
        final View v = inflater.inflate(R.layout.controller, container, false);
        v.findViewById(id.fowardBtn).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                sendCommand(connect, "_0;s" + speed + ';');
            }
        });
        v.findViewById(id.backBtn).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                sendCommand(connect, "_180;s" + speed + ';');
            }
        });
        v.findViewById(id.leftBtn).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                sendCommand(connect, "_90;s" + speed + ';');
            }
        });
        v.findViewById(id.rightBtn).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                sendCommand(connect, "_-90;s" + speed + ';');
            }
        });
        v.findViewById(id.stopBtn).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                sendCommand(connect, "p10;");
            }
        });
        ((VerticalSeekBar) v.findViewById(id.speedBar)).setProgress(this.speed);
        ((VerticalSeekBar) v.findViewById(id.speedBar)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar p0, int progress, boolean fromUser) {
                speed = progress;
                sendCommand(connect, "" + 's' + speed + ';');
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
                sendCommand(connect, cmd);
            }
        }, 200);
        v.findViewById(id.reset_gyro).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                sendCommand(connect, "g20;");
            }
        });

        ((AppCompatSpinner) v.findViewById(id.controller_mode)).setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onNothingSelected(AdapterView p0) {
            }

            public void onItemSelected(AdapterView p0, View p1, int p2, long p3) {
                String mode = ((AppCompatSpinner) v.findViewById(id.controller_mode)).getSelectedItem().toString();
                ToastsKt.toast(getActivity(), mode);
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
                    this.getView().findViewById(id.keyboard_layout).setVisibility(View.VISIBLE);
                    this.getView().findViewById(id.joystickR).setVisibility(View.INVISIBLE);
                }
                break;
            case "Joystick":
                if (mode.equals("Joystick")) {
                    this.getView().findViewById(id.keyboard_layout).setVisibility(View.INVISIBLE);
                    this.getView().findViewById(id.joystickR).setVisibility(View.VISIBLE);
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

    public final void sendCommand(ConnectFragment connect, String command) {
        if (connect != null) {
            connect.log("Try to send " + command);
        }
        try {
            connect.getSocket().getOutputStream().write(command.getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            this.getView().<TextView>findViewById(id.car_info).setText("Not connected");
        }

    }

    public final void slimSendCommand(ConnectFragment connect, String command) {
        if (connect != null) {
            connect.log("Try to send " + command);
        }
        try {
            connect.getSocket().getOutputStream().write(command.getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public final void trigger(int direction) {
        if (direction == -1) {
            this.slimSendCommand(this.connect, "p10;");
        } else {
            int tmp;
            for (tmp = this.currentAngle + direction * 90; tmp < 0; tmp += 360) ;
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
            this.slimSendCommand(this.connect, "" + '_' + tmp + ";s250;");
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
                    String line = new BufferedReader(new InputStreamReader(connect.getSocket().getInputStream())).readLine().trim();
                    Log.d("BLUETOOTHx", "read" + line);
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
                            ((TextView) getView().findViewById(id.car_info)).setText((CharSequence) ("Front: " + frontDistance + " cm\n" + "Back: " + backDistance + '\n' + "Current angle: " + currentAngle + '\n' + "Desired angle: " + desiredAngle));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

}
