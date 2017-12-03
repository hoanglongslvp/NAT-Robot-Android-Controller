package com.worker.natrobotcontroller.activities;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomNavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.crashlytics.android.Crashlytics;
import com.github.kayvannj.permission_utils.Func;
import com.github.kayvannj.permission_utils.PermissionUtil;
import com.worker.natrobotcontroller.R;
import com.worker.natrobotcontroller.R.id;
import com.worker.natrobotcontroller.customView.NoSwipeViewPager;
import com.worker.natrobotcontroller.fragments.CameraSightFragment;
import com.worker.natrobotcontroller.fragments.ConnectFragment;
import com.worker.natrobotcontroller.fragments.JoystickFragment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import kotlin.jvm.internal.Intrinsics;

public final class MainActivity extends AppCompatActivity {
    @Nullable
    public ConnectFragment connect;
    @Nullable
    public JoystickFragment controller;
    @Nullable
    private CameraSightFragment camera;
    private final OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = (OnNavigationItemSelectedListener) (new OnNavigationItemSelectedListener() {
        public final boolean onNavigationItemSelected(@NotNull MenuItem item) {
            MainActivity.this.setTitle(item.getTitle());
            switch (item.getItemId()) {
                case R.id.navigation_connect:
                    ((NoSwipeViewPager) findViewById(id.main_pager)).setCurrentItem(0);
                    return true;
                case R.id.navigation_camera:
                    ((NoSwipeViewPager) findViewById(id.main_pager)).setCurrentItem(1);
                    return true;
                case R.id.navigation_joystick:
                    ((NoSwipeViewPager) findViewById(id.main_pager)).setCurrentItem(2);
                    return true;
            }
            Log.d("Navigation", "nav called " + ((NoSwipeViewPager) findViewById(id.main_pager)).getCurrentItem());
            camera.switchCam(((NoSwipeViewPager) findViewById(id.main_pager)).getCurrentItem() == 1);
            return false;
        }
    });

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setContentView(R.layout.activity_main);
        this.camera = new CameraSightFragment();
        this.connect = new ConnectFragment();
        this.controller = new JoystickFragment();
        NoSwipeViewPager viewPager = findViewById(id.main_pager);
        viewPager.setAdapter(new FragmentPagerAdapter(this.getSupportFragmentManager()) {
            @NotNull
            private final List fragments = new ArrayList();
            {
                fragments.add(MainActivity.this.connect);
                fragments.add(MainActivity.this.camera);
                fragments.add(MainActivity.this.controller);
            }

            @NotNull
            public Fragment getItem(int position) {
                return (Fragment) this.fragments.get(position);
            }

            public int getCount() {
                return this.fragments.size();
            }
        });


        viewPager.addOnPageChangeListener(new OnPageChangeListener() {
            @Nullable
            private MenuItem prev;

            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                if (this.prev != null) {
                    this.prev.setChecked(false);
                } else {
                    ((BottomNavigationView) findViewById(id.navigation)).getMenu().getItem(0).setChecked(false);
                }

                this.prev = ((BottomNavigationView) findViewById(id.navigation)).getMenu().getItem(position);
                if (this.prev != null) {
                    this.prev.setChecked(true);
                }

                Log.d("Navigation", "page called " + position);
                if (MainActivity.this.camera != null) {
                    MainActivity.this.camera.switchCam(position == 1);
                }

            }
        });
        ((NoSwipeViewPager) findViewById(id.main_pager)).setOffscreenPageLimit(3);
        ((BottomNavigationView) findViewById(id.navigation)).setOnNavigationItemSelectedListener(this.mOnNavigationItemSelectedListener);
        this.askForPermission();
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
        Intrinsics.checkParameterIsNotNull(item, "item");
        switch (item.getItemId()) {
            case R.id.setting_action:
                this.startActivity(new Intent(this, SettingActivity.class));
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        Intrinsics.checkParameterIsNotNull(menu, "menu");
        this.getMenuInflater().inflate(R.menu.switch_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
