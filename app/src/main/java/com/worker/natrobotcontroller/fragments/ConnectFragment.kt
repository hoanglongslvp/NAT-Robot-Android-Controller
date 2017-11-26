package com.worker.natrobotcontroller.fragments

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.worker.natrobotcontroller.R
import kotlinx.android.synthetic.main.connect.view.*
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.progressDialog
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.selector
import java.io.IOException
import java.util.*


/**
 * Created by hataketsu on 11/12/17.
 */
class ConnectFragment : Fragment() {
    private val blue = BluetoothAdapter.getDefaultAdapter()
    var isBoned = false
    var device: BluetoothDevice? = null
    var devices = mutableListOf<BluetoothDevice>()
    val myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    var socket: BluetoothSocket? = null
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater?.inflate(R.layout.connect, container, false)
        setupUI(v as View)
        return v
    }

    private fun setupUI(v: View) {
        v.scanBtn.setOnClickListener {
            if (!blue.isEnabled) {
                log("Try to enable bluetooth result: ${blue.enable()}")
            }
            scan()
        }

        v.paired.setOnClickListener {
            devices.clear()
            blue.bondedDevices.forEach { devices.add(it) }
            getConnect()
        }

    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        activity.registerReceiver(mBroadcast, filter)
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(mBroadcast)
    }

    private fun scan() {
        log("Start scanning")
        activity.indeterminateProgressDialog("Start scanning", "Bluetooth").show()
        blue.startDiscovery()
    }

    fun log(s: String) {
        context.runOnUiThread {
//            view?.log?.append(s + "\n")
            Log.d("BLUETOOTH", s)
        }
    }


    private val mBroadcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val mDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    when (mDevice.bondState) {
                        BluetoothDevice.BOND_BONDED -> {
                            device = mDevice
                            isBoned = true
                            log("Boned to device ${device?.name} at address :${device?.address}")
                        }
                        BluetoothDevice.BOND_BONDING -> {
                            isBoned = false
                            log("Boning to device ${device?.name} at address :${device?.address}")
                        }
                        BluetoothDevice.BOND_NONE -> {
                            isBoned = false
                            socket=null
                            log("Broke boning to device ${device?.name} at address :${device?.address}")
                        }
                    }
                }

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                    when (state) {
                        BluetoothAdapter.STATE_OFF -> log("Bluetooth off")
                        BluetoothAdapter.STATE_TURNING_OFF -> log("Bluetooth turning off")
                        BluetoothAdapter.STATE_ON -> log("Bluetooth on")
                        BluetoothAdapter.STATE_TURNING_ON -> log(
                                "Bluetooth turning on")
                    }
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val d = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    devices.add(d)
                    log("Found " + d.name + " at " + d.address)
                    getConnect()
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    devices.clear()
                    log("Started discovering")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    log("Discovery finished")
                    blue.cancelDiscovery()

                }
            }
        }
    }

    private fun getConnect() {
        val names = devices.map { "Devices " + it.name }
        activity.runOnUiThread {
            activity.selector("Select device", names, { dialogInterface, i ->
                device = devices[i]
                log("Select +$i +${names[i]}")
                startConnecting()
                blue.cancelDiscovery()
            })
        }
    }

    private fun startConnecting() {
        activity.run {
            val dialog = progressDialog("Connecting, please wait...", "Bluetooth")
            try {
                runOnUiThread { dialog.show() }
                socket = device?.createInsecureRfcommSocketToServiceRecord(myUUID)
                log("Try to connect to socket")
                socket?.connect()
                log("Connected to socket")
            } catch (e: IOException) {
                e.printStackTrace()
                log("Connect to socket fail : " + e.message)

                socket = null
            } finally {
                dialog.dismiss()
            }
        }
    }

}