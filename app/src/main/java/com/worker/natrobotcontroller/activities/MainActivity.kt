package com.worker.natrobotcontroller.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import com.github.kayvannj.permission_utils.Func
import com.github.kayvannj.permission_utils.PermissionUtil
import com.worker.natrobotcontroller.R
import com.worker.natrobotcontroller.fragments.CameraSightFragment
import com.worker.natrobotcontroller.fragments.ConnectFragment
import com.worker.natrobotcontroller.fragments.JoystickFragment
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    var camera: CameraSightFragment? = null
    var connect: ConnectFragment? = null

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        title = item.title
        when (item.itemId) {
            R.id.navigation_connect -> {
                main_pager.currentItem = 0
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_camera -> {
                main_pager.currentItem = 1
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_joystick -> {
                main_pager.currentItem = 2
                return@OnNavigationItemSelectedListener true
            }
        }
        Log.d("Navigation", "nav called " + main_pager.currentItem)
        camera?.switchCam(main_pager.currentItem == 1)
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //don't let the screen turn off
        setContentView(R.layout.activity_main)

        camera = CameraSightFragment()
        connect = ConnectFragment()
        main_pager.adapter = object : FragmentPagerAdapter(supportFragmentManager) {
            val fragments = listOf(connect as Fragment, camera as Fragment,  JoystickFragment())
            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount() = fragments.size
        }

        main_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            var prev: MenuItem? = null
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                if (prev != null) {
                    prev?.isChecked = false
                } else {
                    navigation.menu.getItem(0).isChecked = false
                }
                prev = navigation.menu.getItem(position)
                prev?.isChecked = true
                Log.d("Navigation", "page called " + position)
                camera?.switchCam(position == 1)
            }

        })
        main_pager.offscreenPageLimit = 3
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        askForPermission()
    }

    private fun askForPermission() {
        PermissionUtil.with(this)
                .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH_PRIVILEGED)
                .onAllGranted(object : Func() {
                    override fun call() {}
                })
                .ask(134) //134 is just a random number
    }

    override fun onResume() {
        super.onResume()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.setting_action -> startActivity(Intent(this, SettingActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.switch_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

}
