package org.schabi.newpipe.brave.misc;

import androidx.annotation.NonNull;

public class BraveRumbleCloudflareEvents {
    public static class EventCloudflareChallengeRequest {

        public final String url;
        public final long timeoutMs;

        public EventCloudflareChallengeRequest(
                final String url,
                final long timeoutMs) {
            this.url = url;
            this.timeoutMs = timeoutMs;
        }

        public interface Handler {
            void handleEventFetchContentRequest(@NonNull EventCloudflareChallengeRequest event);
        }
    }

    public static class EventCloudflareChallengeResponse {
        public final BraveBypassResult result;

        public EventCloudflareChallengeResponse(
                final BraveBypassResult result) {
            this.result = result;
        }

        public interface Handler {
            void handleEventCloudflareChallengeResponse(EventCloudflareChallengeResponse event);
        }
    }

    public static class EventCloudflareServiceReady {

        public EventCloudflareServiceReady() { }

        public interface Handler {
            void handleEventCloudflareServiceReady();
        }
    }

    public static class EventCloudflareServiceShutdown {

        public EventCloudflareServiceShutdown() { }

        public interface Handler {
            void handleEventCloudflareServiceShutdown();
        }
    }

    public static class EventServiceActions {
        public final Actions action;

        public enum Actions {
            ShutdownService,
            MinimizeOverlay,
            InteractiveOverlay
        }

        public EventServiceActions(final Actions action) {
            this.action = action;
        }

        public interface Handler {
            void handleEventServiceActions(EventServiceActions event);
        }
    }

}
