package org.schabi.newpipe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public final class BraveNewVersionWorkerHelper {

    private BraveNewVersionWorkerHelper() {
    }

    public static JsonObject getVersionInfo(
            final String jsonData,
            final String currentFlavor
    ) throws JsonParserException {

        final JsonObject newpipeVersionInfo = JsonParser.object()
                .from(jsonData).getObject("flavors")
                .getObject("github").getObject("stable");

        final Optional<Object> alternativeApkUrlForCurrentFlavor =
                newpipeVersionInfo.getArray("alternative_apks").stream().filter(obj -> {
                    final JsonObject jsonObj = (JsonObject) obj;
                    return jsonObj.getString("alternative").equals(currentFlavor);
                }).findFirst();


        if (alternativeApkUrlForCurrentFlavor.isPresent()) {
            final JsonObject jsonObj = (JsonObject) alternativeApkUrlForCurrentFlavor.get();
            newpipeVersionInfo.put("apk", jsonObj.get("url"));
        }
        return newpipeVersionInfo;
    }

    /**
     *  Create the intent do launch the Activity/Dialog that will download the new APK.
     *
     * @param context        android context
     * @param versionName    the new version string
     * @param apkLocationUrl the url of the apk
     * @param changeLog      a short summary of what has changed
     * @return               the intent described above
     */
    public static Intent getUpgradeActivityIntent(
            final Context context,
            final String versionName,
            final String apkLocationUrl,
            final String changeLog
    ) {
        final BraveUpgradeInfo upgradeInfo =
                new BraveUpgradeInfo(versionName, apkLocationUrl, changeLog);
        final Intent intent = new Intent(context, BraveUpgradeActivity.class);
        intent.putExtra(BraveUpgradeActivity.UPGRADE_INFO, upgradeInfo);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    public static boolean isBraveUpdateBehaviourEnabled(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(
                context.getString(R.string.brave_settings_update_behaviour_key), false);
    }

    @NonNull
    public static Response getUpdateInfo(@NonNull final String apiUpdateUrl)
            throws IOException, ReCaptchaException {
        try {
            final Response response = DownloaderImpl.getInstance().get(apiUpdateUrl);
            if (response.responseCode() == 404) {
                throw new IOException("404 url does not exist");
            }
            return response;

        } catch (final IOException | ReCaptchaException ignored) {

        }
        // the data url before new url is active
        // this method should be removed after move to new github username
        final String  oldUpdateApi =
                "https://raw.githubusercontent.com/bravenewpipe/bnp-r-mgr/master/api/data.json";
        return DownloaderImpl.getInstance().get(oldUpdateApi);
    }

    public static String getProjectUrl() {
        final String newProjPage = "https://github.com/bravepipeproject";
        final String oldProjPage = "https://github.com/bravenewpipe";

        try {
            final Response response = DownloaderImpl.getInstance().head(oldProjPage);
            if (response.responseCode() == 200) {
                return oldProjPage;
            }
        } catch (final IOException | ReCaptchaException e) {
            System.out.println(e);
        }

        return newProjPage;
    }
}
