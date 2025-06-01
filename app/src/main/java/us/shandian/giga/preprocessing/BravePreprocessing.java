package us.shandian.giga.preprocessing;

import us.shandian.giga.get.DownloadMission;

/**
 * Process the mission data before the start of any download.
 */
public interface BravePreprocessing {

    /**
     * Do modifications to the {@link DownloadMission} before starting the download.
     * @param mission contains all information for the download.
     */
    void modifyMission(DownloadMission mission);
}
