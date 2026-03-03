package org.schabi.newpipe;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import org.conscrypt.Conscrypt;
import org.schabi.newpipe.settings.BraveVideoAudioSettingsBaseFragment;
import org.schabi.newpipe.util.BraveTLSSocketFactory;
import coil3.bravePipeLegacy.PicassoHelper;

import java.security.Security;

import androidx.annotation.Nullable;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;

public class BraveApp extends MultiDexApplication
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        Security.insertProviderAt(Conscrypt.newProvider(), 1);
        appContext = getApplicationContext();

        // enable TLS1.2/1.3 for <=kitkat devices, to fix download and play for
        // media.ccc.de, rumble, soundcloud, peertube sources
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            BraveTLSSocketFactory.setAsDefault();
        }

        PicassoHelper.init(appContext);
        PicassoHelper.setIndicatorsEnabled(MainActivity.DEBUG);

        PreferenceManager.getDefaultSharedPreferences(appContext)
                .registerOnSharedPreferenceChangeListener(this);

        makeConfigOptionsSuitableForFlavor();
    }

    /**
     * Get the application context.
     * <p>
     * In the {@link App} from main source set there is the static method {@link App#getInstance()}
     * from which many parts of the application get ApplicationContext. But as the class
     * {@link App} inherits from BraveApp and sets the app variable in {@link @App#onCreate()}
     * only after the {@link BraveApp#onCreate()} is called. Therefore {@link App#getInstance()}
     * has not yet an initialized return valule thus we need another way to get the
     * ApplicationContext
     *
     * @return the application context
     */
    public static Context getAppContext() {
        return appContext;
    }

    private void makeConfigOptionsSuitableForFlavor() {
        BraveVideoAudioSettingsBaseFragment.makeConfigOptionsSuitableForFlavor(getAppContext());
    }

    @Override
    public void onSharedPreferenceChanged(
            final SharedPreferences sharedPreferences,
            final @Nullable String configOption
    ) {
        if (configOption == null) {
            return;
        }
        if (configOption.equals(
                appContext.getString(R.string.brave_settings_host_replace_key))) {
            PicassoHelper.reInit(appContext);
        }
    }
}
