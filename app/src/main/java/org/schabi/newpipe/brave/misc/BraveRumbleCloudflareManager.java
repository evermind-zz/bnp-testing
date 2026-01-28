package org.schabi.newpipe.brave.misc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.schabi.newpipe.brave.bus.BraveBus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Wrap around BypassClient to tell cloudflare, that a human is using the app.
 */
@SuppressLint("StaticFieldLeak")
public final class BraveRumbleCloudflareManager
        implements BraveRumbleCloudflareEvents.EventCloudflareChallengeResponse.Handler,
        BraveRumbleCloudflareManagerInterface {

    public static final boolean DBG_CF = true; // enable to see some debug messages
    private BraveRumbleCloudflareWebViewHandler handler;

    private volatile CountDownLatch challengeEventResultLatch = null;
    private BraveBypassResult eventResult = null;

    @Override
    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void handleEventCloudflareChallengeResponse(
            final BraveRumbleCloudflareEvents.EventCloudflareChallengeResponse event) {
        eventResult = event.result;
        if (challengeEventResultLatch != null) {
            challengeEventResultLatch.countDown();
        } else {
            throw new RuntimeException(
                    "challengeEventResultLatch == null -> that should never happen");
        }
    }

    private static volatile BraveRumbleCloudflareManager instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WebView webView;
    private volatile String currentCookies = "";

    private BraveRumbleCloudflareManager(final Context context) {
        final Context appContext = context.getApplicationContext();
        BraveBus.getBus().register(this);

        if (Looper.myLooper() != Looper.getMainLooper()) {
            final CountDownLatch initLatch = new CountDownLatch(1);
            mainHandler.post(() -> {
                createWebView(appContext);
                initLatch.countDown();
            });
            try {
                initLatch.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            createWebView(appContext);
        }
    }

    public static BraveRumbleCloudflareManager getInstance(final Context context) {
        if (instance == null) {
            synchronized (BraveRumbleCloudflareManager.class) {
                if (instance == null) {
                    instance = new BraveRumbleCloudflareManager(context);
                }
            }
        }
        return instance;
    }

    private void createWebView(final Context context) {
        webView = new WebView(context);
        webView.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
        webView.setVisibility(View.GONE);

        handler = new BraveRumbleCloudflareWebViewHandler(webView);
    }

    @Override
    public synchronized BraveBypassResult fetchContentViaWebView(
            final String url,
            final long timeoutMs) {
        challengeEventResultLatch = new CountDownLatch(1);
        //handler.fetchContentViaWebView(url, timeoutMs, true, () -> startDebugActivity(url));
        handler.fetchContentViaWebView(url, timeoutMs);

        //try {
        //    // the debugLatch will wait until we receive an event from the the handler
        //    challengeEventResultLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        //} catch (final InterruptedException e) {
        //    Thread.currentThread().interrupt();
        //    return new BypassResult(false, null, "");
        //}
        try {
            challengeEventResultLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();  // Status erhalten
            // Warte trotzdem weiter (schlecht, aber funktioniert)
            try {
                challengeEventResultLatch.await();  // unendlich warten nach Interrupt
            } catch (final InterruptedException ignored) {
                return new BraveBypassResult(false, null, "");

            }
        }

        if (eventResult != null) {
            return new BraveBypassResult(
                    eventResult.success(),
                    eventResult.content(),
                    eventResult.cookies()
            );
        } else {
            return new BraveBypassResult(false, null, "");
        }
    }


    @Override
    public String getCurrentCookies() {
        return currentCookies;
    }

    @Override
    public void destroy() {
        BraveBus.getBus().unregister(this);
        handler.destroy();
        mainHandler.post(() -> {
            if (webView != null) {
                webView.destroy();
            }
            instance = null;
        });
    }
}
