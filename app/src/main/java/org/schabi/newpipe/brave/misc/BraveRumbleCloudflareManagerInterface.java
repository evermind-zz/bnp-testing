package org.schabi.newpipe.brave.misc;

public interface BraveRumbleCloudflareManagerInterface {

    BraveBypassResult fetchContentViaWebView(String url, long timeoutMs);

    String getCurrentCookies();

    void destroy();
}
