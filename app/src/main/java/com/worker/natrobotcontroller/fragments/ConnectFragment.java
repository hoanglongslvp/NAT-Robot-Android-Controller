package com.worker.natrobotcontroller.fragments;

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
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.worker.natrobotcontroller.R;
import com.worker.natrobotcontroller.R.id;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ConnectFragment extends Fragment {
    private final BluetoothAdapter blue = BluetoothAdapter.getDefaultAdapter();
    private final UUID myUUID;
    private final BroadcastReceiver mBroadcast;
    private BluetoothDevice device;
    private List<BluetoothDevice> devices;
    public BluetoothSocket socket;


    public ConnectFragment() {
        this.devices = new ArrayList<>();
        this.myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        this.mBroadcast = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        switch (state) {
                            case BluetoothAdapter.STATE_OFF:
                                log("Bluetooth off");
                                break;
                            case BluetoothAdapter.STATE_TURNING_OFF:
                                log("Bluetooth turning off");
                                break;
                            case BluetoothAdapter.STATE_ON:
                                log("Bluetooth on");
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
                        blue.cancelDiscovery();
                }
            }
        };
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.connect, container, false);
        this.setupUI(v);
        return v;

    }

    private void setupUI(View v) {
        v.findViewById(id.scanBtn).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                if (!blue.isEnabled()) {
                    log("Try to enable bluetooth result: " + blue.enable());
                }

                scan();
            }
        });
        v.findViewById(id.paired).setOnClickListener(new OnClickListener() {
            public final void onClick(View it) {
                devices.clear();
                devices.addAll(blue.getBondedDevices());
                getConnect();
            }
        });
    }

    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.bluetooth.device.action.BOND_STATE_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        filter.addAction("android.bluetooth.device.action.FOUND");
        filter.addAction("android.bluetooth.adapter.action.DISCOVERY_STARTED");
        filter.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
        this.getActivity().registerReceiver(this.mBroadcast, filter);
    }

    public void onPause() {
        super.onPause();
        this.getActivity().unregisterReceiver(this.mBroadcast);
    }

    private void scan() {
        this.log("Start scanning");
        this.blue.startDiscovery();
    }

    public final void log(final String s) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("BLUETOOTH", s);
            }
        });
    }

    private void getConnect() {
        final List<String> names = new ArrayList();
        for (BluetoothDevice d : devices) {
            names.add("Devices " + d.getName());
        }

        final CharSequence[] _names = new CharSequence[names.size()];
        names.toArray(_names);
        this.getActivity().runOnUiThread(new Runnable() {
            public final void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setSingleChoiceItems(_names, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        device = devices.get(i);
                        log("Select +" + i + " +" + names.get(i));
                        startConnecting();
                        blue.cancelDiscovery();
                    }
                }).show();
            }
        });
    }

    private void startConnecting() {

        try {
            this.socket = this.device != null ? this.device.createInsecureRfcommSocketToServiceRecord(this.myUUID) : null;
            this.log("Try to connect to socket");
            this.socket.connect();
            this.log("Connected to socket");
        } catch (IOException e) {
            e.printStackTrace();
            this.log("Connect to socket fail : " + e.getMessage());
            this.socket = null;
        }

    }
}
