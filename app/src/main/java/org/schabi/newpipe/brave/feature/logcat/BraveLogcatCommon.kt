package org.schabi.newpipe.brave.feature.logcat

import com.github.logviewer.ExportLogFileUtils
import com.github.logviewer.settings.CleanupConfig
import com.github.logviewer.settings.KeepLastNFilesStrategy

/**
 * have same cleanup strategy within logcat-toolkit' UI calls and its logcat dumper.
 */
fun commonCleanupStrategy(): CleanupConfig = CleanupConfig(KeepLastNFilesStrategy(), 10)

/**
 * use common log storage location within logcat-toolkit' UI calls and its logcat dumper.
 */
fun commonStorageLocation(): ExportLogFileUtils.StorageLocation = ExportLogFileUtils.StorageLocation.CACHE_INTERNAL
