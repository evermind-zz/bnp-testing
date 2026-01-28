package org.schabi.newpipe.brave.misc;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.ead.lib.cloudflare_bypass.BypassClient;

import org.schabi.newpipe.App;
import org.schabi.newpipe.DownloaderImpl;
import org.schabi.newpipe.brave.bus.BraveBus;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.schabi.newpipe.brave.misc.BraveRumbleCloudflareManager.DBG_CF;

public class BraveRumbleCloudflareWebViewHandler {

    private final WebView webView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final String[] cookieDomains = {
            "https://rumble.com",
            "rumble.com",
            ".rumble.com",
            "https://www.rumble.com",
            "www.rumble.com"
    };
    private volatile String currentCookies = "";
    public BraveRumbleCloudflareWebViewHandler(
            final WebView webView) {
        this.webView = webView;
        setupWebView();
    }

    public void setupWebView() {
        final WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUserAgentString(DownloaderImpl.USER_AGENT);

        final String versionInfo = Logcat.getDetailedWebViewVersion(
                App.getApp().getApplicationContext());
        Log.d("CF_DBG", "WebView Info – " + versionInfo);
        setupWebViewConsoleLogging();
    }

    private void setupWebViewConsoleLogging() {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(final ConsoleMessage consoleMessage) {
                final String msg = String.format("WebViewConsoleLog: [%s:%d] %s",
                        consoleMessage.sourceId(),
                        consoleMessage.lineNumber(),
                        consoleMessage.message());
                Log.d("CF_DBG", msg);
                return true;
            }
        });
    }

    private void callableSafeCaller(
            final Callable<Void> callable) {
        try {
            callable.call();
        } catch (final Exception e) {
            Log.d("CF_DBG", "this should never happen", e);
            throw new RuntimeException("");
        }
    }
            public synchronized void fetchContentViaWebView(
            final String url,
            final long timeoutMs) {

        // own thread as we we have to wait for the main UI thread to do stuff
        final Thread fetchContentThread = new Thread(() -> {
            swagger(url, timeoutMs);
        });
        fetchContentThread.setName("FetchContentThread");
        fetchContentThread.start();
    }
    private synchronized void swagger(
            final String url,
            final long timeoutMs) {

        final CountDownLatch latch = new CountDownLatch(1);
        final String[] resultContent = {null};
        final String[] resultCookies = {""};
        final boolean[] success = {false};

        try {
            mainHandler.post(() -> {
                webView.setWebViewClient(new BypassClient() {
                    @Override
                    public void onPageFinishedByPassed(
                            final WebView view,
                            final String loadedUrl) {
                        super.onPageFinishedByPassed(view, loadedUrl);
                        Log.d("CF_DBG", "BypassClient.onPageFinishedByPassed(): "
                                + "view=" +      view
                                + "loadedUrl=" + loadedUrl);

                        final boolean isResultJson = loadedUrl.contains("embedJS");
                        final String script = isResultJson
                                ? "document.body.textContent || document.body.innerText"
                                : "document.documentElement.outerHTML";

                        webView.evaluateJavascript(
                                "(function() { return " + script + "; })();",
                                value -> {
                                    Log.d("CF_DBG", "start WebView.evaluateJavascript(): "
                                            + "value=" + value
                                            + "script=" + script);
                                    if (value != null && !value.equals("null")) {
                                        // Remove the surrounding quotation marks
                                        String raw = value.substring(1, value.length() - 1);
                                        if (!isResultJson) {
                                            raw = BraveStringEscapeUtils.unescapeJava(raw);
                                        }
                                        resultContent[0] = raw.trim();
                                    }
                                    resultCookies[0] = updateAndGetCookies();
                                    success[0] = true;
                                    Log.d("CF_DBG", "end WebView.evaluateJavascript(): "
                                            + "success=" + true);
                                    latch.countDown();
                                }
                        );
                    }

                    /**
                     *  We use the deprecated version as we want only main page errors.
                     *
                     * @param view The WebView that is initiating the callback.
                     * @param errorCode The error code corresponding to an ERROR_* value.
                     * @param description A String describing the error.
                     * @param failingUrl The url that failed to load.
                     */
                    @Override
                    public void onReceivedError(
                            final WebView view,
                            final int errorCode,
                            final String description,
                            final String failingUrl) {
                        super.onReceivedError(view, errorCode, description, failingUrl);
                        Log.d("CF_DBG", "BypassClient.onReceivedError(): "
                                + "view=" +      view
                                + "errorCode=" + errorCode
                                + "description=" + description
                                + "failingUrl=" + failingUrl);
                        latch.countDown();
                    }
                });
                Log.d("CF_DBG", "start webView.loadUrl(" + url + ")");
                webView.loadUrl(url);
                Log.d("CF_DBG", "end webView.loadUrl(" + url + ")");
            });

            dumpCookiesForKnownDomains();

            final boolean completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (!(completed && success[0])) {
                // TODO maybe just send here an error message to retry
                Log.d("CF_DBG", "(!(completed && success[])");
            } else { // best scenario would be that branch
                BraveBus.getBus().post(
                        new BraveRumbleCloudflareEvents.EventCloudflareChallengeResponse(
                                new BraveBypassResult(
                                        completed && success[0],
                                        resultContent[0],
                                        resultCookies[0]
                                )
                        )
                );
            }
            return;
        } catch (final InterruptedException e) {
            Log.d("CF_DBG", "Catched InterruptException: ", e);
            Thread.currentThread().interrupt();
            //if (callable != null) {
            //    try {
            //        return callable.call();
            //    } catch (final Exception ex) {
            //        Log.d("CF_DBG", "this should never happen", ex);
            //    }
            //}
            //return newNoSuccessByPassResult();
        }

        BraveBus.getBus().post(new BraveRumbleCloudflareEvents.
                EventCloudflareChallengeResponse(newNoSuccessByPassResult())
        );
    }

    private BraveBypassResult newNoSuccessByPassResult() {
        return new BraveBypassResult(false, null, currentCookies);
    }

    private String updateAndGetCookies() {
        String selectedCookies = null;

        for (final String domain : cookieDomains) {
            final String cookies = CookieManager.getInstance().getCookie(domain);
            if (cookies != null && !cookies.isEmpty()) {
                if (cookies.contains("__cf")) {
                    selectedCookies = cookies;
                    break;  // cancel we found CF-Cookie
                }
                if (selectedCookies == null) {
                    selectedCookies = cookies;  // fallback: first none empty cookie
                }
            }
        }

        currentCookies = (selectedCookies != null) ? selectedCookies : "";
        return currentCookies;
    }

    private void dumpCookiesForKnownDomains() {
        if (!DBG_CF) {
            return;
        }
        final CookieManager manager = CookieManager.getInstance();
        flushCookieManager(manager);

        for (final String domain : cookieDomains) {
            final String cookies = manager.getCookie(domain);
            if (cookies != null && !cookies.isEmpty()) {
                Log.d("CF_DBG COOKIES", domain + " => " + cookies);
            }
        }
    }

    private void flushCookieManager(final CookieManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            manager.flush();
        } else {
            final CookieSyncManager cookieSyncManager =
                    CookieSyncManager.createInstance(App.getApp().getApplicationContext());
            cookieSyncManager.sync();
        }
    }

    public void destroy() {
        webView.setWebViewClient(null);
       //TODO figure out what to do here  -- eg stop thread etc
    }
}
