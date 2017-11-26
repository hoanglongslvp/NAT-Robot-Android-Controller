package com.worker.natrobotcontroller.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import com.worker.natrobotcontroller.R
import com.worker.natrobotcontroller.activities.MainActivity
import kotlinx.android.synthetic.main.controller.view.*
import kotlinx.android.synthetic.main.joystick.view.*
import org.jetbrains.anko.doAsync
import java.io.IOException

/**
 * Created by hataketsu on 11/12/17.
 */
class JoystickFragment : Fragment() {
    var mode = "Joystick"
    var speed = 255
    var lastCommand = ""
    var connect_status: TextView? = null

    var frontDistance = 0
    var backDistance = 0
    var currentAngle = 0
    var desiredAngle = 0

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val connect = (activity as MainActivity).connect
        val v: View?
        if (mode == "Joystick") {
            v = inflater?.inflate(R.layout.joystick, container, false)
            v!!.joystickR.setOnMoveListener({ angle, strength ->
                val pwm = strength * 255 / 100
                val cmd = "_${angle - 90};s$pwm;"
                sendCommand(connect, cmd)
            }, 200)
            connect_status = v.connect_status
        } else {
            v = inflater?.inflate(R.layout.controller, container, false)
            v!!.fowardBtn.setOnClickListener { sendCommand(connect, format("_0;")) }
            v.backBtn.setOnClickListener { sendCommand(connect, format("_180;")) }
            v.leftBtn.setOnClickListener { sendCommand(connect, format("_90;")) }
            v.rightBtn.setOnClickListener { sendCommand(connect, format("_-90;")) }
            v.stopBtn.setOnClickListener { sendCommand(connect, format("p10;")) }
            v.speedBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                    speed = progress
                    sendCommand(connect, "s$speed;")
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                }

            })
            connect_status = v.connect_statusc
        }
        activity.doAsync {
            while (true) {
                try {
                    if (connect?.socket?.inputStream != null) {
                        if (connect.socket?.isConnected!!) {
                            val line = connect.socket?.inputStream?.bufferedReader()?.readLine()?.trim()
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
                                connect_status?.setText("Front : $frontDistance cm")
                            }
                        }
                    } else Thread.sleep(1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return v
    }

    private fun readNumber(s: String): Int {
        return s.substring(1, s.length).toInt()
    }

    private fun format(s: String): String {
        lastCommand = s
        return s.format(speed, speed)
    }

    override fun onResume() {
        super.onResume()
        reloadSetting()
    }

    private fun reloadSetting() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val _mode = preferences.getString("control_mode", "Joystick")
        if (mode != _mode) {
            mode = _mode
            fragmentManager.beginTransaction().detach(this).attach(this).commit()
        }
    }

    private fun sendCommand(connect: ConnectFragment?, command: String) {
        connect?.log("Try to send $command")
        if (connect?.socket == null) {
            connect_status?.setText("Not connected")
        } else
            try {
                connect.socket?.outputStream?.write(command.toByteArray())
                connect_status?.setText("Connected")
                connect.log("Sent")
            } catch (e: IOException) {
                e.printStackTrace()
                connect.log("Send command error " + e.message)
                connect_status?.setText("Not connected")
            }
    }

}