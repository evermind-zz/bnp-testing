package org.schabi.newpipe.download;

import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.List;

import androidx.fragment.app.DialogFragment;

import static org.schabi.newpipe.extractor.stream.DeliveryMethod.HLS;
import static org.schabi.newpipe.util.ListHelper.getStreamsOfSpecifiedDelivery;

public class BraveDownloadDialog extends DialogFragment {

    protected List<VideoStream> braveAddHlsStreams(
            final StreamInfo info,
            final List<VideoStream> videoStreams) {
        videoStreams.addAll(getStreamsOfSpecifiedDelivery(info.getVideoStreams(), HLS));

        return videoStreams;
    }

    protected boolean braveIsHlsStream(
            final Stream selectedStream) {
        return selectedStream.getDeliveryMethod() == HLS;
    }
}
