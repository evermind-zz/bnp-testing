package org.schabi.newpipe.util;

import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

public final class BraveStreamInfoWrapperHelper {
    private BraveStreamInfoWrapperHelper() {
    }

    /**
     * check if stream is HLS and if so set the size of the stream.
     * <p>
     * As HLS streams the length cannot be determined by using HEAD and extracting Content-Length
     * we have to calc a approximate size.
     *
     * @param stream         the stream we want to calculate the download size
     * @param streamInfo     overall metadata that applies to all streams.
     * @param streamsWrapper the wrapper
     * @param <X>            the stream type to get the {@link MediaFormat}.
     * @return true if HLS stream and could calculate stream size
     */
    public static <X extends Stream> boolean braveIfStreamIsHlsCalcAndSetSize(
            final X stream,
            final StreamInfo streamInfo,
            final StreamItemAdapter.StreamInfoWrapper<X> streamsWrapper) {
        if (stream.getDeliveryMethod() == DeliveryMethod.HLS) {
            final long approxSize = braveCalcSize(streamInfo, stream);
            if (approxSize > 0) {
                streamsWrapper.setSize(stream, approxSize);
                return true;
            }
        }
        return false;
    }

    private static <X extends Stream> long braveCalcSize(
            final StreamInfo streamInfo,
            final X stream) {
        if (stream instanceof VideoStream videoStream) {
            final long bitrate = videoStream.getBitrate();
            final long duration = streamInfo.getDuration();
            if (bitrate > 0 && duration > 0) {
                // 8 to convert bits to Bytes
                final long approxDownloadSizeInBytes = (bitrate / 8) * duration;
                return approxDownloadSizeInBytes;
            }
        }
        return -1; // size could not be calculated
    }
}
