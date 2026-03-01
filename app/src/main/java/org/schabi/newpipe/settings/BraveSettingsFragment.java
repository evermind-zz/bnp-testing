package org.schabi.newpipe.settings;

import android.os.Bundle;

import org.schabi.newpipe.App;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

public class BraveSettingsFragment extends BasePreferenceFragment {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResourceRegistry();
    }

    @Override
    public boolean onPreferenceTreeClick(@NonNull final Preference preference) {
        App.getInstance().getExtractorSettings().initExtractorConfig();
        return super.onPreferenceTreeClick(preference);
    }
}
