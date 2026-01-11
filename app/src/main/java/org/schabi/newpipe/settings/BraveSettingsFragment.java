package org.schabi.newpipe.settings;

import android.os.Bundle;

import com.github.logviewer.LogcatActivity;

import org.schabi.newpipe.App;
import org.schabi.newpipe.R;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

public class BraveSettingsFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResourceRegistry();
        setupLogcatViewer();
    }

    @Override
    public boolean onPreferenceTreeClick(@NonNull final Preference preference) {
        App.getApp().getExtractorSettings().initExtractorConfig();
        return super.onPreferenceTreeClick(preference);
    }


    private void setupLogcatViewer() {
        final Preference pref = findPreference(requireContext().getString(
                R.string.brave_settings_debug_logcat_viewer_key));
        if (null != pref) {
            pref.setOnPreferenceClickListener(preference -> {
                        LogcatActivity.Companion.start(
                                requireContext()
                        );
                        return true;
                    }
            );
        }
    }
}
