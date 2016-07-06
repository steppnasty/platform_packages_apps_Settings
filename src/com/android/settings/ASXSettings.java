package com.android.settings;

import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import java.util.HashSet;

public class ASXSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String ADVANCED_REBOOT = "advanced_reboot";
    private static final String TRACKBALL_WAKE = "trackball_wake";
    private static final String KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";

    private CheckBoxPreference mAdvancedReboot;
    private CheckBoxPreference mTrackballWake;
    private CheckBoxPreference mKillAppLongpressBack;

    private final HashSet<Preference> mDisabledPrefs = new HashSet<Preference>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.aospsx_settings);

        mAdvancedReboot = (CheckBoxPreference) findPreference(ADVANCED_REBOOT);
        mAdvancedReboot.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ADVANCED_REBOOT, 0) != 0);
        mTrackballWake = (CheckBoxPreference) findPreference(TRACKBALL_WAKE);
        mTrackballWake.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.TRACKBALL_WAKE_SCREEN, 1) == 1);
        /* Remove mTrackballWake on devices without trackballs */
        if (!getResources().getBoolean(R.bool.has_trackball))
            getPreferenceScreen().removePreference(mTrackballWake);
        mKillAppLongpressBack = (CheckBoxPreference)
                findPreference(KILL_APP_LONGPRESS_BACK);
        mKillAppLongpressBack.setChecked(Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.Secure.KILL_APP_LONGPRESS_BACK, 1) == 1);

        if (!android.os.Process.myUserHandle().equals(UserHandle.OWNER)) {
            disableForUser(mAdvancedReboot);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen prefScreen,
            Preference pref) {
        if (pref == mAdvancedReboot) {
            writeAdvancedRebootOptions();
            return true;
        } else if (pref == mTrackballWake) {
            writeTrackballWakeOptions();
            return true;
        } else if (pref == mKillAppLongpressBack) {
            writeKillAppLongpressBackOptions();
            return true;
        }
        return super.onPreferenceTreeClick(prefScreen, pref);
    }

    private void disableForUser(Preference pref) {
        if (pref != null) {
            pref.setEnabled(false);
            mDisabledPrefs.add(pref);
        }
    }

    private void writeAdvancedRebootOptions() {
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.Secure.ADVANCED_REBOOT,
                mAdvancedReboot.isChecked() ? 1 : 0);
    }

    private void writeTrackballWakeOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.TRACKBALL_WAKE_SCREEN,
                mTrackballWake.isChecked() ? 1 : 0);
    }

    private void writeKillAppLongpressBackOptions() {
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.Secure.KILL_APP_LONGPRESS_BACK,
                mKillAppLongpressBack.isChecked() ? 1 : 0);
    }

}
