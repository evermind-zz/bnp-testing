package org.schabi.newpipe.brave.misc;

import java.io.IOException;
import java.util.Optional;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Handle 403 in case cloudflare questions a human using the app.
 */
public class BraveRumble403Interceptor implements Interceptor {

    private static final MediaType HTML = MediaType.get("text/html; charset=utf-8");
    private final BraveRumbleCloudflareManager bypassManager;

    public BraveRumble403Interceptor(final BraveRumbleCloudflareManager manager) {
        this.bypassManager = manager;
    }

    public static Optional<Interceptor> getInterceptor(
            final OkHttpClient.Builder builder) {
        return builder.interceptors().stream().filter(
                BraveRumble403Interceptor.class::isInstance).findFirst();
    }

    @NonNull
    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();

        if (!request.url().host().contains("rumble.com")) {
            return chain.proceed(request);
        }

        //USELESS->DISABLED // reuse previously retrieved cookies from the webView
        final String cookies = ""; //USELESS->DISABLED bypassManager.getCurrentCookies();
        //USELESS->DISABLED final Request.Builder builder = request.newBuilder();
        //USELESS->DISABLED if (!cookies.isEmpty()) {
        //USELESS->DISABLED     builder.header("Cookie", cookies);
        //USELESS->DISABLED }

        //USELESS->DISABLED final Response response = chain.proceed(builder.build());
        final Response response = chain.proceed(request);

        if (response.code() == 200) {
            return response;
        }

        if (response.code() == 403) {
            final BraveRumbleCloudflareManager.BypassResult bypassResult =
                    bypassManager.fetchContentViaWebView(request.url().toString(), 30000);

            if (bypassResult.success() && bypassResult.content() != null) {
                // reuse the webView's content as a proper okHttp response.
                final Request newRequest = request.newBuilder().build();
                return new Response.Builder()
                        .request(newRequest)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(HTML, bypassResult.content()))
                        .build();
            }
        }

        return response;
    }
}
