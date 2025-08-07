package us.shandian.giga.postprocessing;

import android.content.Context;

import com.github.evermindzz.hlsdownloader.FFmpegSegmentCombiner;
import com.github.evermindzz.hlsdownloader.HlsMediaProcessor;
import com.github.evermindzz.hlsdownloader.parser.HlsParser;
import com.github.evermindzz.slimhls.converter.RunFFmpeg;

import org.schabi.newpipe.App;
import org.schabi.newpipe.streams.io.SharpInputStream;
import org.schabi.newpipe.streams.io.SharpOutputStream;
import org.schabi.newpipe.streams.io.SharpStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import okio.Source;
import us.shandian.giga.get.DownloadMission;

public class BraveFromHlsRemuxer extends Postprocessing {

    BraveFromHlsRemuxer() {
        super(false, true, ALGORITHM_BRAVE_HLS_REMUXER);
    }


    @Override
    int process(SharpStream out, SharpStream... sources) throws IOException {
        try {
            BraveHlsConverterToMp4 converterToMp4 = new BraveHlsConverterToMp4(this.mission);
            converterToMp4.convert(sources, out);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return OK_RESULT;
    }

    /**
     * Provide the hls segment based on the index.
     */
    static final class SegmentInputStreamProvider implements HlsMediaProcessor.InputStreamProvider {
        final SharpStream[] segmentStreams;

        private SegmentInputStreamProvider(SharpStream[] segmentStreams) {
            this.segmentStreams = segmentStreams;
        }

        @Override
        public InputStream get(URI uri, int i) throws IOException {
            if (segmentStreams.length > i) {
                return new SharpInputStream(segmentStreams[i]);
            }
            throw new RuntimeException("there is not stream for index: " + i);
        }
    }

    private static class BraveHlsConverterToMp4 {

        private final File tempDir;
        private final DownloadMission mission;

        BraveHlsConverterToMp4(DownloadMission mission) {
            this.mission = mission;
            final Context context = App.getApp().getApplicationContext();
            this.tempDir = new File(context.getExternalFilesDir(null), "ffmpeg_temp");
        }

        private FFmpegSegmentCombiner createCombiner() {
            RunFFmpeg runFFmpeg = new RunFFmpeg(App.getApp().getApplicationContext());
            return new FFmpegSegmentCombiner(args -> {
                try {
                    return runFFmpeg.execute(args);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        /**
         * Actually convert the hls stream to the output stream
         *
         * @param segmentStreams        the hls input segment streams
         * @param outputStream          the result of the conversion will be written there
         * @throws IOException          on I/O problems. eg writing to disk etc.
         * @throws InterruptedException could happen from {@link HlsMediaProcessor} but
         *                              not likely here as we do not interrupt it.
         */
        public void convert(
                SharpStream[] segmentStreams,
                SharpStream outputStream)
                throws IOException, InterruptedException {
            createTempDir();

            File remuxedFile = new File(tempDir, "output.mp4");
            SegmentInputStreamProvider segmentInputStreamProvider = new SegmentInputStreamProvider(segmentStreams);
            HlsMediaProcessor hlsMediaProcessor = new HlsMediaProcessor(null, tempDir.getAbsolutePath(), remuxedFile.getAbsolutePath(),
                    null,
                    null,
                    1, null,
                    createCombiner(),
                    (progress, total) -> {
                    },
                    (state, message) -> {
                    },
                    true
            );

            if (mission.braveArbitraryData instanceof List<?>) {
                @SuppressWarnings("unchecked") final List<HlsParser.Segment> segments = (List<HlsParser.Segment>) mission.braveArbitraryData;
                hlsMediaProcessor.setSegments(segments);
                hlsMediaProcessor.processSegments(segmentInputStreamProvider);
                hlsMediaProcessor.finalizeDownload();

                copyFileToStream(remuxedFile, outputStream);
            }

            cleanupTempFiles(remuxedFile);
        }

        private void copyFileToStream(
                File inFile,
                SharpStream outStream) throws IOException {
            Source source = Okio.source(inFile);
            SharpOutputStream sharpOutputStream = new SharpOutputStream(outStream);
            Sink sink = Okio.sink(sharpOutputStream);

            BufferedSource bufferedSource = Okio.buffer(source);
            BufferedSink bufferedSink = Okio.buffer(sink);

            // Read all data from source to sink
            bufferedSource.readAll(bufferedSink);

            // Flush the buffered sink to ensure all data is pushed through
            bufferedSink.flush();

            // close only the source. The 'out' stream will be properly closed by the
            // Postprocessing framework in CircularFileWriter.finalizeFile().
            // If we close it here the framework would crash as it can no longer finalize it
            bufferedSource.close();
        }

        private void createTempDir() {
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
        }

        private void cleanupTempFiles(
                final File outputFile) {
            outputFile.delete();
            tempDir.delete();
        }
    }
}
