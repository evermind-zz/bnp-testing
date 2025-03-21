package org.schabi.newpipe;

public class BraveCommonApp extends BraveApp {
    private BraveExtractorSettings extractorSettings;
    @Override
    public void onCreate() {
        super.onCreate();
        extractorSettings = new BraveExtractorSettings(this);
        extractorSettings.initExtractorConfig();
    }

    public BraveExtractorSettings getExtractorSettings() {
        return extractorSettings;
    }
}
