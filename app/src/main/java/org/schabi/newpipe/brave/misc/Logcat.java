package org.schabi.newpipe.brave.misc;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public final class Logcat {
    private Logcat() {
    }

    public static String getLogcatDump(
            final int noOfMessages,
            final String packageSignature) {
        try {
            final Process process =
                    Runtime.getRuntime().exec("logcat -d -t " + noOfMessages + " "
                            + packageSignature + "*:V");
            final BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            process.waitFor();
            return sb.toString();
        } catch (final Exception e) {
            return "Logcat-Dump failed: " + e.getMessage();
        }
    }

        private static String getWebViewPackageInfo(
                final Context context
        ) {

        final PackageManager pm = context.getPackageManager();
        try {
            final PackageInfo info = pm.getPackageInfo("com.google.android.webview", 0);
            // or "com.android.webview" on some devices
            return info.packageName + " v" + info.versionName;
        } catch (final PackageManager.NameNotFoundException e) {
            try {
                final PackageInfo info =
                        pm.getPackageInfo("com.android.webview", 0);
                return info.packageName + " v" + info.versionName;
            } catch (final PackageManager.NameNotFoundException ex) {
                return "System WebView (version unknown)";
            }
        }
    }

    // Tries to get detailed Chromium version via reflection (works on many devices)
    public static String getDetailedWebViewVersion(
            final Context context) {
        try {
            // Try to get Chromium version via reflection (common field)
            final Class<?> webViewFactory = Class.forName("android.webkit.WebViewFactory");
            final Method getProvider = webViewFactory.getMethod("getProvider");
            final Object provider = getProvider.invoke(null);
            final Class<?> providerClass = provider.getClass();
            final Method getVersion = providerClass.getMethod("getVersion");
            final String chromiumVersion = (String) getVersion.invoke(provider);

            return String.format(
                    "Package: %s | Chromium: %s", getWebViewPackageInfo(context), chromiumVersion);
        } catch (final Exception e) {
            return "Package: " + getWebViewPackageInfo(context) + " (Chromium version unknown)";
        }
    }

}
