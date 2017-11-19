package com.worker.natrobotcontroller.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.worker.natrobotcontroller.R
import com.worker.natrobotcontroller.activities.MainActivity
import kotlinx.android.synthetic.main.joystick.view.*
import java.io.IOException

/**
 * Created by hataketsu on 11/12/17.
 */
class JoystickFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater?.inflate(R.layout.joystick, container, false)
        setupUI(v as View)
        return v
    }

    private fun setupUI(v: View) {
        val connect = (activity as MainActivity).connect
        v.joystickL.setOnMoveListener({ angle, strength ->
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

    }

    private fun sendCommand(connect: ConnectFragment?, command: String) {
        if (connect?.socket == null) {
            connect?.log("Not connected to send $command")
        } else

            try {
                connect.socket?.getOutputStream()?.write(command.toByteArray())
//                connect.log("Sent command $command")
            } catch (e: IOException) {
                e.printStackTrace()
//                connect.log("Send command error " + e.message)
            }
    }

}