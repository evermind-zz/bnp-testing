package org.schabi.newpipe.player.resolver;

import android.util.Log;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamType;

public abstract class BraveVideoPlaybackResolver implements PlaybackResolver {
    private final VideoPlaybackResolver.QualityResolver resolver;

    BraveVideoPlaybackResolver(
            final VideoPlaybackResolver.QualityResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * NewPipe does not choose a specific quality for live streams.
     * <p>
     * On Rumble we want to choose the quality the user specified in
     * the settings.
     *
     * TODO basically NewPipe should handle the quality of live streams
     *
     * @param info modify its hls url according to the selected quality
     */
    protected void braveChangeQualityOnRumbleLiveStreams(
            final StreamInfo info) {
        if (info.getStreamType() == StreamType.LIVE_STREAM
                && info.getServiceId() == ServiceList.Rumble.getServiceId()) {
            final int index =
                    resolver.getDefaultResolutionIndex(info.getVideoStreams());
            final String qualityHlsUrl =
                    info.getVideoStreams().get(index).getManifestUrl();
            if (qualityHlsUrl != null && !qualityHlsUrl.isBlank()) {
                info.setHlsUrl(qualityHlsUrl);
            } else {
                Log.w(TAG, "could not set set hls url according to select quality");
            }
        }
    }
}
