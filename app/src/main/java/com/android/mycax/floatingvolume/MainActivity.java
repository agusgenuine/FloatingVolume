package com.android.mycax.floatingvolume;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import com.android.mycax.floatingvolume.services.FloatingVolumeService;
import com.android.mycax.floatingvolume.utils.AppUtils;
import com.android.mycax.floatingvolume.utils.Constants;

import java.util.Objects;

import mehdi.sakout.fancybuttons.FancyButton;
import mehdi.sakout.fancybuttons.Utils;

@SuppressLint("ExportedPreferenceActivity")
@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatPreferenceActivity implements SwitchPreference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, FancyButton.OnClickListener {
    private FancyButton FloatingServiceStart;
    private FancyButton FloatingServiceStop;
    private ListPreference dialogPosition;
    private ListPreference headOpacityPreference;
    private ListPreference floatingIconSizePref;
    private AppUtils utils;
    private NotificationManager notificationManager;
    private SharedPreferences sharedPref;
    private SwitchPreference ringerSwitch;
    private int theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        utils = new AppUtils(this);
        theme = Integer.valueOf(Objects.requireNonNull(sharedPref.getString(Constants.PREF_THEME_VALUE, "1")));
        utils.onActivityCreateSetTheme(this, theme);
        if (theme == Constants.THEME_CUSTOM) {
            utils.setActionBarTextColor(getSupportActionBar());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FloatingServiceStart = findViewById(R.id.button_start_service);
        FloatingServiceStop = findViewById(R.id.button_stop_service);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            checkPermissions();
        } else {
            initializeView();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_main);
        ListPreference themePreference = (ListPreference) findPreference(Constants.PREF_THEME_VALUE);
        themePreference.setOnPreferenceChangeListener(this);
        Preference customThemePreference = findPreference(Constants.PREF_CUSTOM_THEME);
        if (theme == Constants.THEME_CUSTOM) {
            customThemePreference.setEnabled(true);
            customThemePreference.setOnPreferenceClickListener(this);
        } else {
            customThemePreference.setEnabled(false);
        }
        ListPreference interfaceType = (ListPreference) findPreference(Constants.PREF_INTERFACE_TYPE);
        interfaceType.setOnPreferenceChangeListener(this);
        headOpacityPreference = (ListPreference) findPreference(Constants.PREF_HEAD_OPACITY);
        headOpacityPreference.setOnPreferenceChangeListener(this);
        dialogPosition = (ListPreference) findPreference(Constants.PREF_DIALOG_POSTITION);
        if (sharedPref.getBoolean(Constants.PREF_DISABLE_FIXED_UI, false))
            dialogPosition.setEnabled(false);
        SwitchPreference disableFixedUI = (SwitchPreference) findPreference(Constants.PREF_DISABLE_FIXED_UI);
        disableFixedUI.setOnPreferenceChangeListener(this);
        Preference aboutPreference = findPreference(Constants.PREF_ABOUT_ME);
        aboutPreference.setOnPreferenceClickListener(this);
        ringerSwitch = (SwitchPreference) findPreference(Constants.PREF_SHOW_MODE_SWITCH);
        ringerSwitch.setOnPreferenceChangeListener(this);
        floatingIconSizePref = (ListPreference) findPreference(Constants.PREF_FLOATING_ICON_SIZE);
        floatingIconSizePref.setOnPreferenceChangeListener(this);
    }

    private void initializeView() {
        FloatingServiceStart.setOnClickListener(this);
        FloatingServiceStop.setOnClickListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        switch (preference.getKey()) {
            case Constants.PREF_DISABLE_FIXED_UI:
                dialogPosition.setEnabled(sharedPref.getBoolean(Constants.PREF_DISABLE_FIXED_UI, false));
                break;
            case Constants.PREF_THEME_VALUE:
                utils.applyTheme(this);
                break;
            case Constants.PREF_FLOATING_ICON_SIZE:
            case Constants.PREF_HEAD_OPACITY:
                if (utils.isServiceRunning(FloatingVolumeService.class)) {
                    utils.manageService(false);
                    utils.manageService(true);
                }
                break;
            case Constants.PREF_SHOW_MODE_SWITCH:
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1 && !ringerSwitch.isChecked()) {
                    checkModeSwitchAvailable();
                }
                break;
            case Constants.PREF_INTERFACE_TYPE:
                if (sharedPref.getString(Constants.PREF_INTERFACE_TYPE, "1") == Constants.ROCKER_SERVICE) {
                    //if(utils.isAccessibilitySettingsOn(getApplicationContext())) startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    startActivityForResult(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS), 6);
                    floatingIconSizePref.setEnabled(false);
                    headOpacityPreference.setEnabled(false);
                } else {
                    floatingIconSizePref.setEnabled(true);
                    headOpacityPreference.setEnabled(true);
                }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkModeSwitchAvailable() {
        if (!Objects.requireNonNull(notificationManager).isNotificationPolicyAccessGranted()) {
            checkPermissions();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Settings.canDrawOverlays(this)) {
            initializeView();
        } if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, Constants.OVERLAY_PERMISSION_REQUEST);
        } if (!Objects.requireNonNull(notificationManager).isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(
                    android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

            startActivityForResult(intent, Constants.NOTIFICATION_POLICY_PERMISSION_REQUEST);
        }

        /*Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_PHONE_STATE)
                .withListener(new PermissionListener() {
                    @Override public void onPermissionGranted(PermissionGrantedResponse response) {/ ... /}
                    @Override public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(getApplicationContext(), R.string.app_permission_denied, Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check(); */
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.OVERLAY_PERMISSION_REQUEST) {
            if (Settings.canDrawOverlays(this)) {
                initializeView();
            } else {
                Toast.makeText(this, R.string.app_permission_denied, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == Constants.NOTIFICATION_POLICY_PERMISSION_REQUEST) {
            if (!Objects.requireNonNull(notificationManager).isNotificationPolicyAccessGranted()) {
                ringerSwitch.setChecked(false);
                //permission not available
            }
        } else if (requestCode == Constants.THEME_PREFRENCES_REQUEST) {
            utils.applyTheme(this);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case Constants.PREF_ABOUT_ME:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case Constants.PREF_CUSTOM_THEME:
                startActivityForResult(new Intent(this, ThemeSettingsActivity.class), Constants.THEME_PREFRENCES_REQUEST);
                break;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_start_service:
                utils.manageService(true);
                break;
            case R.id.button_stop_service:
                utils.manageService(false);
                break;
        }
    }
}
