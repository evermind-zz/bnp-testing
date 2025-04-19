package org.schabi.newpipe;

import org.schabi.newpipe.brave.bus.BraveSharedPrefsListenerToEventsBridge;

import androidx.preference.PreferenceManager;

public class BraveCommonApp extends BraveApp {
    private BraveExtractorSettings extractorSettings;

    // keep a reference otherwise the listener will be garbage collected
    // sharedpreferences-onsharedpreferencechangelistener-not-being-called-consistently
    // -> https://stackoverflow.com/questions/2542938
    //
    private BraveSharedPrefsListenerToEventsBridge onPrefsChangeListener;

    @Override
    public void onCreate() {
        super.onCreate();
        extractorSettings = new BraveExtractorSettings(this);
        extractorSettings.initExtractorConfig();
        onPrefsChangeListener = new BraveSharedPrefsListenerToEventsBridge(getApplicationContext());

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .registerOnSharedPreferenceChangeListener(onPrefsChangeListener);
    }

    public BraveExtractorSettings getExtractorSettings() {
        return extractorSettings;
    }
}
