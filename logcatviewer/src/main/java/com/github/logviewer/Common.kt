package com.github.logviewer

import android.content.Context
import android.content.Intent
import com.github.logviewer.databinding.LogcatViewerFragmentLogcatBinding
import com.google.android.material.snackbar.Snackbar
import de.brudaswen.android.logcat.core.parser.LogcatBinaryParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedInputStream
import java.util.regex.Pattern

object Common {

    @JvmStatic
    val EXCLUDE_LIST_KEY = "exclude_list"

    @JvmStatic
    fun runParser(
        excludeList: MutableList<Pattern>,
        onNewItem: (LogItem) -> Unit,
        doRead: () -> Boolean
    ) = runBlocking {
        val processPid = android.os.Process.myPid()
        val process = ProcessBuilder("logcat", "-B", "--pid=" + processPid).start()

        LogcatBinaryParser(
            input = BufferedInputStream(process.inputStream)
        ).use { parser ->
            while (doRead()) {
                val logcatItem = parser.parseItem()
                val item = LogItem(logcatItem)

                if (excludeList.any { pattern -> pattern.matcher(item.origin).matches() }) {
                    continue
                }

                onNewItem(item)
            }
        }
    }

    @JvmStatic
    fun exportLog(
        binding: LogcatViewerFragmentLogcatBinding,
        adapter: LogcatAdapter,
        context: Context
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val exportedFile = ExportLogFileUtils.exportLogs(
                context.externalCacheDir, adapter.data
            )
            if (exportedFile == null) {
                Snackbar.make(
                    binding.root,
                    R.string.logcat_viewer_create_log_file_failed,
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                shareIntent.setType("text/plain")
                val uri = LogcatFileProvider.getUriForFile(
                    context,
                    "${context.packageName}.logcat_fileprovider",
                    exportedFile
                )
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                if (context.packageManager.queryIntentActivities(shareIntent, 0).isEmpty()) {
                    Snackbar.make(
                        binding.root,
                        R.string.logcat_viewer_not_support_on_this_device,
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    context.startActivity(shareIntent)
                }
            }
        }
    }
}