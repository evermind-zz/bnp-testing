package org.schabi.newpipe.brave.misc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.schabi.newpipe.App;
import org.schabi.newpipe.brave.bus.BraveBus;
import org.schabi.newpipe.brave.service.BraveFloatingWebViewService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

/**
 * Wrap around BypassClient to tell cloudflare, that a human is using the app.
 */
@SuppressLint("StaticFieldLeak")
public final class BraveRumbleCloudflareServiceManager
        implements BraveRumbleCloudflareManagerInterface,
        BraveRumbleCloudflareEvents.EventCloudflareChallengeResponse.Handler,
        BraveRumbleCloudflareEvents.EventCloudflareServiceReady.Handler {

    public static final boolean DBG_CF = true; // enable to see some debug messages

    private volatile CountDownLatch eventStartCloudflareServiceLatch = null;
    private volatile CountDownLatch eventCloudflareChallengeResponseLatch = null;
    private BraveBypassResult eventResult = null;

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    @Override
    public void handleEventCloudflareChallengeResponse(
            @NonNull final BraveRumbleCloudflareEvents.EventCloudflareChallengeResponse event) {
        eventResult = event.result;
        if (eventCloudflareChallengeResponseLatch != null) {
            eventCloudflareChallengeResponseLatch.countDown();
        } else {
            throw new RuntimeException(
                    "challengeEventResultLatch == null -> that should never happen");
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    @Override
    public void handleEventCloudflareServiceReady() {

        if (eventStartCloudflareServiceLatch != null) {
            eventStartCloudflareServiceLatch.countDown();
        } else {
            throw new RuntimeException(
                    "eventStartCloudflareServiceLatch == null -> that should never happen");
        }
    }

    private static volatile BraveRumbleCloudflareServiceManager instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile String currentCookies = "";

    private void startCloudflareChallengeService() {
        final Context context = App.getApp().getApplicationContext();
        final Intent intent = new Intent(context, BraveFloatingWebViewService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private BraveRumbleCloudflareServiceManager(final Context context) {
        BraveBus.getBus().register(this);
    }

    public static BraveRumbleCloudflareServiceManager getInstance(final Context context) {
        if (instance == null) {
            synchronized (BraveRumbleCloudflareServiceManager.class) {
                if (instance == null) {
                    instance = new BraveRumbleCloudflareServiceManager(context);
                }
            }
        }
        return instance;
    }

    @Override
    public synchronized BraveBypassResult fetchContentViaWebView(
            final String url,
            final long timeoutMs) {
        eventStartCloudflareServiceLatch = new CountDownLatch(1);
        eventCloudflareChallengeResponseLatch = new CountDownLatch(1);
        startCloudflareChallengeService();
        try {
            eventStartCloudflareServiceLatch.await(5000, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // TODO put this one into the handleEventCloudflareServiceReady() -- or maybe not
        BraveBus.getBus().post(
                new BraveRumbleCloudflareEvents.EventCloudflareChallengeRequest(url, timeoutMs)
        );
        BraveBus.getBus().post(new BraveRumbleCloudflareEvents.EventServiceActions(
                BraveRumbleCloudflareEvents.EventServiceActions.Actions.InteractiveOverlay
        ));

        try {
            eventCloudflareChallengeResponseLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BraveBypassResult(false, null, "");
        }

        if (eventResult != null) {
            if (eventResult.success()) {
                BraveBus.getBus().post(new BraveRumbleCloudflareEvents.EventServiceActions(
                        BraveRumbleCloudflareEvents.EventServiceActions.Actions.MinimizeOverlay
                ));
            }
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
        BraveBus.getBus().post(new BraveRumbleCloudflareEvents.EventServiceActions(
                BraveRumbleCloudflareEvents.EventServiceActions.Actions.ShutdownService
        ));
        BraveBus.getBus().unregister(this);
        mainHandler.post(() -> { //TODO remove singleton
            instance = null;
        });
    }
}
