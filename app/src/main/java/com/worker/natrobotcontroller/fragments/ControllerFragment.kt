package com.worker.natrobotcontroller.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.worker.natrobotcontroller.R
import com.worker.natrobotcontroller.activities.MainActivity
import kotlinx.android.synthetic.main.controller.view.*
import java.io.IOException

/**
 * Created by hataketsu on 11/12/17.
 */
class ControllerFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater?.inflate(R.layout.controller, container, false)
        setupUI(v as View)
        return v
    }

    private fun setupUI(v: View) {
        val connect = (activity as MainActivity).connect
        v.fowardBtn.setOnClickListener { sendCommand(connect, 'l') }
        v.backBtn.setOnClickListener { sendCommand(connect, '2') }
        v.leftBtn.setOnClickListener { sendCommand(connect, '3') }
        v.rightBtn.setOnClickListener { sendCommand(connect, '4') }
        v.stopBtn.setOnClickListener { sendCommand(connect, '6') }
    }

    private fun sendCommand(connect: ConnectFragment?, command: Char) {
//        val arr = CharArray(1)
//        arr[0] = command
        if (connect?.socket == null) {
            connect?.log("Not connected")
        } else

            try {
                connect.socket?.getOutputStream()?.write(command.toInt())
                connect.log("Sent command $command")
            } catch (e: IOException) {
                e.printStackTrace()
                connect.log("Send command error " + e.message)
            }
    }

}