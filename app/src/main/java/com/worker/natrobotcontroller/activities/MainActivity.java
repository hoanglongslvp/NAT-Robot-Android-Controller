package com.worker.natrobotcontroller.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.github.kayvannj.permission_utils.Func;
import com.github.kayvannj.permission_utils.PermissionUtil;
import com.worker.natrobotcontroller.R;
import com.worker.natrobotcontroller.R.id;
import com.worker.natrobotcontroller.components.CameraSightFragment;
import com.worker.natrobotcontroller.components.JoystickFragment;

import org.jetbrains.anko.ToastsKt;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MainActivity extends AppCompatActivity {

    private final BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
    public JoystickFragment controller;
    public BluetoothSocket socket;
    private List<BluetoothDevice> devices = new ArrayList<>();
    private CameraSightFragment camera;
    private final OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = (OnNavigationItemSelectedListener) (new OnNavigationItemSelectedListener() {
        public final boolean onNavigationItemSelected(@NotNull MenuItem item) {
            setTitle(item.getTitle());
            switch (item.getItemId()) {
                case R.id.navigation_camera:
                    camera.show();
                    controller.hide();
                    return true;
                case R.id.navigation_joystick:
                    camera.hide();
                    controller.show();
                    return true;
            }
            return false;
        }
    });
    private BottomNavigationView navigationView;
    private MenuItem toggleItem;
    private MenuItem networkIndicator;
    BroadcastReceiver mBroadcast = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            log("Bluetooth off");
                            setToggle(false);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            log("Bluetooth turning off");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            log("Bluetooth on");
                            setToggle(true);
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            log("Bluetooth turning on");
                            break;
                    }
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(newDevice);
                    log("Found " + newDevice.getName() + " at " + newDevice.getAddress());
                    getConnect();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    devices.clear();
                    log("Started discovering");
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    log("Discovery finished");
                    bluetooth.cancelDiscovery();
            }
        }
    };

    private void setToggle(boolean b) {
        if (toggleItem != null) {
            if (b) {
                toggleItem.setTitle("Turn off bluetooth");
            } else
                toggleItem.setTitle("Turn on bluetooth");
        }
    }

    public void log(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastsKt.toast(getApplication(), s);
                Log.d("NATCAR", s);
            }
        });
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        controller = new JoystickFragment(this, findViewById(id.controller_view));
        camera = new CameraSightFragment(this, findViewById(id.camera_sight_view));
        navigationView = findViewById(id.navigation);
        navigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigationView.setSelectedItemId(R.id.navigation_joystick);
//        camera.hide();
//        controller.show();
        askForPermission();
    }


    private void askForPermission() {
        PermissionUtil.with(this).request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_PRIVILEGED,
                Manifest.permission.INTERNET).onAllGranted(new Func() {
            protected void call() {
            }
        }).ask(134);//134 is just a random number
    }

    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        switch (item.getItemId()) {
            case id.action_scan:
                if (bluetooth.isEnabled()) {
                    bluetooth.startDiscovery();
                } else {
                    ToastsKt.toast(this, "Bluetooth is not turned on!");
                }
                break;
            case id.action_paired:
                devices.clear();
                devices.addAll(bluetooth.getBondedDevices());
                getConnect();
                break;
            case id.action_toggle:
                toogleBluetooth();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void toogleBluetooth() {
        if (bluetooth.isEnabled()) {
            bluetooth.disable();
        } else {
            bluetooth.enable();
        }
    }

    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        getMenuInflater().inflate(R.menu.switch_menu, menu);
        toggleItem = menu.findItem(id.action_toggle);
        setToggle(bluetooth.isEnabled());
        networkIndicator = menu.findItem(id.network_indicator);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        filter.addAction("android.bluetooth.device.action.FOUND");
        filter.addAction("android.bluetooth.adapter.action.DISCOVERY_STARTED");
        filter.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
        registerReceiver(mBroadcast, filter);
        controller.startReading();
    }

    public void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcast);
        controller.stopReading();
    }

    private void getConnect() {
        final ArrayList<String> names = new ArrayList<>();
        for (BluetoothDevice d : devices) {
            names.add("Devices " + d.getName());
        }

        final CharSequence[] _names = new CharSequence[names.size()];
        names.toArray(_names);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog alertDialog = builder.setSingleChoiceItems(_names, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startConnecting(devices.get(i));
                bluetooth.cancelDiscovery();
                dialogInterface.dismiss();
            }
        }).create();
        alertDialog.show();
    }

    private void startConnecting(final BluetoothDevice device) {
        log("Try to connect to socket");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    socket.connect();
                    log("Connected to socket");
                    setOnline();
                } catch (IOException e) {
                    e.printStackTrace();
                    log("Connect to socket fail : " + e.getMessage());
                    socket = null;
                    setOffline();
                }
            }
        }).start();
    }

    public void setOffline() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                networkIndicator.setIcon(R.drawable.ic_signal_wifi_off_black_24dp);
            }
        });
    }

    public void setOnline() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                networkIndicator.setIcon(R.drawable.ic_signal_wifi_4_bar_black_24dp);
            }
        });
    }
}

