package gr.thmmy.mthmmy.activities.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;

import gr.thmmy.mthmmy.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String ARG_IS_LOGGED_IN = "selectedRingtoneKey";

    //Preferences xml keys
    private static final String POSTING_CATEGORY = "pref_category_posting_key";
    private static final String SELECTED_NOTIFICATIONS_SOUND = "pref_notifications_select_sound_key";

    //SharedPreferences keys
    private static final int REQUEST_CODE_ALERT_RINGTONE = 2;
    public static final String SETTINGS_SHARED_PREFS = "settingsSharedPrefs";
    public static final String SELECTED_RINGTONE = "selectedRingtoneKey";
    private static final String SILENT_SELECTED = "STFU";

    private SharedPreferences settingsFile;

    private boolean isLoggedIn = false;

    public static SettingsFragment newInstance(boolean isLoggedIn) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_LOGGED_IN, isLoggedIn);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null){
            isLoggedIn = args.getBoolean(ARG_IS_LOGGED_IN,false);
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        // Load the Preferences from the XML file
        addPreferencesFromResource(R.xml.app_preferences);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findPreference(POSTING_CATEGORY).setVisible(isLoggedIn);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(SELECTED_NOTIFICATIONS_SOUND)) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);

            Activity activity = this.getActivity();
            settingsFile = activity != null
                    ? activity.getSharedPreferences(SETTINGS_SHARED_PREFS, Context.MODE_PRIVATE)
                    : null;
            String existingValue = settingsFile != null
                    ? settingsFile.getString(SELECTED_RINGTONE, null)
                    : null;
            if (existingValue != null) {
                if (existingValue.equals(SILENT_SELECTED)) {
                    //Selects "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
                }
            } else {
                //No ringtone has been selected, set to the default
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
            }

            startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE);
            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ALERT_RINGTONE && data != null) {
            Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            SharedPreferences.Editor editor = settingsFile.edit();
            if (ringtone != null) {
                editor.putString(SELECTED_RINGTONE, ringtone.toString()).apply();
            } else {
                //"Silent" was selected
                editor.putString(SELECTED_RINGTONE, SILENT_SELECTED).apply();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void updateUserLoginState(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }
}