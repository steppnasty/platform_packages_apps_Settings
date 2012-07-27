package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.IWindowManager;

import java.util.HashSet;

public class ASXSettings extends SettingsPreferenceFragment implements
        DialogInterface.OnClickListener, DialogInterface.OnDismissListener,
        Preference.OnPreferenceChangeListener {

    private static final String ADVANCED_REBOOT = "advanced_reboot";
    private static final String TRACKBALL_WAKE = "trackball_wake";
    private static final String KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";
    private static final String ROOT_ACCESS_KEY = "root_access";
    private static final String ROOT_ACCESS_PROPERTY = "persist.sys.root_access";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";

    private CheckBoxPreference mAdvancedReboot;
    private CheckBoxPreference mTrackballWake;
    private CheckBoxPreference mKillAppLongpressBack;
    private ListPreference mRootAccess;
    private Object mSelectedRootValue;
    private Dialog mRootDialog;

    private final HashSet<Preference> mDisabledPrefs = new HashSet<Preference>();

    private boolean mDialogClicked;

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
        mRootAccess = (ListPreference) findPreference(ROOT_ACCESS_KEY);
        mRootAccess.setOnPreferenceChangeListener(this);
        removeRootOptionsIfRequired();
        updateRootAccessOptions();

        if (!android.os.Process.myUserHandle().equals(UserHandle.OWNER)) {
            disableForUser(mAdvancedReboot);
        }

        // Only show the hardware keys config on a device that does not have a navbar
        IWindowManager windowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
            if (windowManager.hasNavigationBar()) {
                getPreferenceScreen().removePreference(findPreference(KEY_HARDWARE_KEYS));
            }
        } catch (RemoteException e) {
            // Do nothing
        }

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    private void removeRootOptionsIfRequired() {
        // user builds don't get root, and eng always gets root
        if (!Build.IS_DEBUGGABLE || "eng".equals(Build.TYPE)) {
            if (mRootAccess != null) {
                getPreferenceScreen().removePreference(mRootAccess);
            }
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mRootDialog) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                writeRootAccessOptions(mSelectedRootValue);
            } else {
                // Reset the option
                writeRootAccessOptions("0");
            }
        }
    }

    public void onDismiss(DialogInterface dialog) {
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRootAccess) {
            if ("0".equals(SystemProperties.get(ROOT_ACCESS_PROPERTY, "1"))
                    && !"0".equals(newValue)) {
                mSelectedRootValue = newValue;
                mDialogClicked = false;
                if (mRootDialog != null) {
                    dismissDialogs();
                }
                mRootDialog = new AlertDialog.Builder(getActivity())
                        .setMessage(getResources().getString(R.string.root_access_warning_message))
                        .setTitle(R.string.root_access_warning_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNegativeButton(android.R.string.no, this).show();
                mRootDialog.setOnDismissListener(this);
            } else {
                writeRootAccessOptions(newValue);
            }
            return true;
        }
        return false;
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

    private void updateRootAccessOptions() {
        String value = SystemProperties.get(ROOT_ACCESS_PROPERTY, "1");
        mRootAccess.setValue(value);
        mRootAccess.setSummary(getResources()
                .getStringArray(R.array.root_access_entries)[Integer.valueOf(value)]);
    }

    private void writeRootAccessOptions(Object newValue) {
        String oldValue = SystemProperties.get(ROOT_ACCESS_PROPERTY, "1");
        SystemProperties.set(ROOT_ACCESS_PROPERTY, newValue.toString());
        if (Integer.valueOf(newValue.toString()) < 2 && !oldValue.equals(newValue)
                && "1".equals(SystemProperties.get("service.adb.root", "0"))) {
            SystemProperties.set("service.adb.root", "0");
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ADB_ENABLED, 0);
            Settings.Secure.putInt(getActivity().getContentResolver(),
                    Settings.Secure.ADB_ENABLED, 1);
        }
        updateRootAccessOptions();
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

    private void dismissDialogs() {
        if (mRootDialog != null) {
            mRootDialog.dismiss();
            mRootDialog = null;
        }
    }

}
