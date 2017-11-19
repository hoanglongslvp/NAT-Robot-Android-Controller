package com.worker.natrobotcontroller.activities

import android.os.Bundle
import android.os.PersistableBundle
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import com.worker.natrobotcontroller.R


class SettingActivity : PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        getActionBar().setDisplayShowTitleEnabled(true);
        title="NAT Setting"
    }

    override fun onBuildHeaders(target: MutableList<Header>?) {
        super.onBuildHeaders(target)
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    class Prefs1Fragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.main_setting)
        }
    }

}
