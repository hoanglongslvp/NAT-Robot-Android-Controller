package com.worker.natrobotcontroller.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SeekBar
import com.worker.natrobotcontroller.R
import com.worker.natrobotcontroller.activities.MainActivity
import kotlinx.android.synthetic.main.controller.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import java.io.IOException

/**
 * Created by hataketsu on 11/12/17.
 */
class JoystickFragment : Fragment() {
    var mode = ""
    var speed = 200
    var frontDistance = 0
    var backDistance = 0
    var currentAngle = 0
    var desiredAngle = 0
    lateinit var connect: ConnectFragment
    val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    var reading = true

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val _connect = (activity as MainActivity).connect
        this.connect = _connect!!

        val v = inflater?.inflate(R.layout.controller, container, false)
        v!!.fowardBtn.setOnClickListener { sendCommand(connect, "_0;s$speed;") }
        v.backBtn.setOnClickListener { sendCommand(connect, "_180;s$speed;") }
        v.leftBtn.setOnClickListener { sendCommand(connect, "_90;s$speed;") }
        v.rightBtn.setOnClickListener { sendCommand(connect, "_-90;s$speed;") }
        v.stopBtn.setOnClickListener { sendCommand(connect, "p10;") }
        v.speedBar.progress = speed
        v.speedBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                speed = progress
                sendCommand(connect, "s$speed;")
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}

        })
        v.joystickR.setOnMoveListener({ angle, strength ->
            val pwm = strength * 255 / 100
            val cmd = "_${angle - 90};s$pwm;"
            sendCommand(connect, cmd)
        }, 200)
        v.reset_gyro.setOnClickListener {
            sendCommand(connect, "g20;")
        }


        v.controller_mode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val mode = v.controller_mode.selectedItem.toString()
                activity.toast(mode)
                selectMode(mode)
                preferences.edit().putString("control_mode", mode).apply()
            }

        }
        selectMode(mode)

        return v
    }

    private fun startReading() {
        activity.doAsync {
            while (reading) {
                try {
                    if (connect.socket?.inputStream != null) {
                        if (connect.socket?.isConnected!!) {
                            val line=connect.socket?.inputStream?.bufferedReader()?.readLine()?.trim()
                            Log.d("BLUETOOTHx", "read" + line)
                            if (!line.isNullOrEmpty()) {
                                line!!.split(";").forEach { cmd ->
                                    if (!cmd.isEmpty())
                                        when (cmd[0]) {
                                            'f' -> frontDistance = readNumber(cmd)
                                            'b' -> backDistance = readNumber(cmd)
                                            'c' -> currentAngle = readNumber(cmd)
                                            'd' -> desiredAngle = readNumber(cmd)
                                        }
                                }

                            }
                            activity.runOnUiThread {
                                view?.car_info?.setText("Front: $frontDistance cm\n" +
                                        "Back: $backDistance\n" +
                                        "Current angle: $currentAngle\n" +
                                        "Desired angle: $desiredAngle")
                            }
                        }
                    } else
                        try {
                            Thread.sleep(1000)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun selectMode(mode: String) {
        when (mode) {
            "Joystick" -> {
                view?.keyboard_layout?.visibility = View.INVISIBLE
                view?.joystickR?.visibility = View.VISIBLE
            }
            "Game pad" -> {
                view?.keyboard_layout?.visibility = View.VISIBLE
                view?.joystickR?.visibility = View.INVISIBLE
            }
        }
    }

    private fun readNumber(s: String): Int {
        return s.substring(1, s.length).toInt()
    }

    override fun onResume() {
        super.onResume()
        reloadSetting()
        reading = true
        startReading()
    }

    override fun onPause() {
        super.onPause()
        reading = false
    }

    private fun reloadSetting() {
        mode = preferences.getString("control_mode", "Joystick")
    }

    private fun sendCommand(connect: ConnectFragment?, command: String) {
        connect?.log("Try to send $command")
        if (connect?.socket == null) {
            view?.car_info?.setText("Not connected")
        } else
            try {
                connect.socket?.outputStream?.write(command.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
                view?.car_info?.setText("Not connected")
            }
    }

    private fun slimSendCommand(connect: ConnectFragment?, command: String) {
        connect?.log("Try to send $command")
        if (connect?.socket != null)
            try {
                connect.socket?.outputStream?.write(command.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
    }

    fun trigger(direction: Int) {
        var tmp = currentAngle + direction * 90
        while (tmp < 0) tmp += 360
        while (tmp > 360) tmp -= 360
        if (tmp < 45)
            tmp = 0
        else if (tmp < 45 + 90)
            tmp = 90
        else if (tmp < 45 + 90 + 90)
            tmp = 90 + 90
        else if (tmp < 45 + 90 + 90 + 90)
            tmp = 90 + 90 + 90
        slimSendCommand(connect, "_$tmp;s250;")
    }


}