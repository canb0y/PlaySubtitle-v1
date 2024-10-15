package com.itbn.playsubtitle.v1.settings;

import android.os.Bundle;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import com.itbn.playsubtitle.v1.settings.NumberPickerPreference;
import com.itbn.playsubtitle.v1.ActivityHelper;
import com.itbn.playsubtitle.v1.R;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    private static SettingsFragment settings;
    private SharedPreferences languagePreference;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_preferences);
        settings = this;
        ActivityHelper.onSettingsCreated = true;
        languagePreference = PreferenceManager.getDefaultSharedPreferences(getContext());
        
        disableConfigurationSetting(ActivityHelper.configAvailability);
        
        languagePreference.registerOnSharedPreferenceChangeListener(this);
    }
    
    public static synchronized SettingsFragment getInstance() {
        return settings;
    }
     
    private void setPreferenceSummary(Preference _preference, String _value) {
        if (_preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) _preference;
            int prefIndex = listPreference.findIndexOfValue(_value);
            if (prefIndex >= 0) {
                listPreference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else {
            if (_preference instanceof EditTextPreference) {
                _preference.setSummary(_value);
            }
        }
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences _sharedPreferences, String _key) {
        Preference preference = findPreference(_key);
        
        if (preference != null) {
            if (preference instanceof ListPreference) {
                String value = _sharedPreferences.getString(preference.getKey(), "");
                setPreferenceSummary(preference, value);
            } else if (preference instanceof EditTextPreference) {
                String value = _sharedPreferences.getString(preference.getKey(), "");
                setPreferenceSummary(preference, value);
            } else if (preference instanceof NumberPickerPreference) {
                int value = _sharedPreferences.getInt(preference.getKey(), 20);
                preference.setSummary("" + value);
            } else if (preference instanceof SwitchPreference) {
                boolean z = _sharedPreferences.getBoolean(preference.getKey(), true);
                preference.setSummary(z ? "Enabled" : "Disabled");
            }
        } 
    }
    
    public void disableConfigurationSetting(boolean z) {
        if (isAdded()) {
            findPreference(getString(R.string.config_key)).setEnabled(z);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        ActivityHelper.onSettingsCreated = false;
        languagePreference.unregisterOnSharedPreferenceChangeListener(this);
    }
}
