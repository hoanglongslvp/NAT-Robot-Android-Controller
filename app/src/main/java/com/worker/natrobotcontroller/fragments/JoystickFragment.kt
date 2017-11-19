package com.worker.natrobotcontroller.fragments

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
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
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val connect = (activity as MainActivity).connect
        val v: View?
        if (mode == "Joystick") {
            v = inflater?.inflate(R.layout.joystick, container, false)
            v!!.joystickL.setOnMoveListener({ angle, strength ->
                val pwm = strength * 255.0 / 100
                var l = 0.0
                if (angle <= 180)
                    l = pwm
                else
                    l = -pwm
                val cmd = "l${l.toInt()};"
                sendCommand(connect, cmd)
            }, 200)

            v.joystickR.setOnMoveListener({ angle, strength ->
                val pwm = strength * 255.0 / 100
                var r = 0.0
                if (angle <= 180)
                    r = pwm
                else
                    r = -pwm
                val cmd = "r${r.toInt()};"
                sendCommand(connect, cmd)
            }, 200)
            connect_status = v.connect_status
        } else {
            v = inflater?.inflate(R.layout.controller, container, false)
            v!!.fowardBtn.setOnClickListener { sendCommand(connect, format("l%d;r%d;")) }
            v.backBtn.setOnClickListener { sendCommand(connect, format("l-%d;r-%d;")) }
            v.leftBtn.setOnClickListener { sendCommand(connect, format("l%d;r-%d;")) }
            v.rightBtn.setOnClickListener { sendCommand(connect, format("l-%d;r%d;")) }
            v.stopBtn.setOnClickListener { sendCommand(connect, format("l0;r0;")) }
            v.speedBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                    speed = progress
                    sendCommand(connect, lastCommand.format(speed, speed))
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
                val line = connect?.socket?.inputStream?.bufferedReader()?.readLine()
                activity.runOnUiThread { connect_status?.setText("Front : $line cm") }
                Thread.sleep(300)
            }
        }
        return v
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
        if (connect?.socket == null) {
            connect_status?.setText("Not connected")
        } else

            try {
                connect.socket?.outputStream?.write(command.toByteArray())
                connect_status?.setText("Connected")
            } catch (e: IOException) {
                e.printStackTrace()
                connect.log("Send command error " + e.message)
                connect_status?.setText("Not connected")
            }
    }

}