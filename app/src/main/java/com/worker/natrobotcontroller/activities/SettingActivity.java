package com.worker.natrobotcontroller.activities;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceFragment;

import com.worker.natrobotcontroller.R;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

import kotlin.Metadata;

public final class SettingActivity extends AppBarSetting {

    protected boolean isValidFragment(@Nullable String fragmentName) {
        return true;
    }

    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        this.getActionBar().setDisplayShowTitleEnabled(true);
        this.setTitle("NAT Setting");
    }

    public void onBuildHeaders(@Nullable List target) {
        super.onBuildHeaders(target);
        this.loadHeadersFromResource(R.xml.preference_headers, target);
    }

    public static final class Prefs1Fragment extends PreferenceFragment {

        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.addPreferencesFromResource(R.xml.main_setting);
        }

    }
}
