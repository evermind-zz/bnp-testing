package org.schabi.newpipe.error

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.github.logviewer.LogcatFileProvider
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.brave.feature.logcat.BraveLogcatDumper.BraveLogFileNameErrorInfo
import org.schabi.newpipe.brave.feature.logcat.BraveLogcatDumper.Companion.isLogcatDumperEnabled
import org.schabi.newpipe.brave.feature.logcat.BraveLogcatDumper.Companion.logcatDump
import org.schabi.newpipe.util.external_communication.ShareUtils

abstract class BraveErrorActivity : AppCompatActivity() {

    /**
     * Skip some traces as we might get TransactionTooLargeException exception.
     *
     * @param stackTraces the full stack traces
     * @return the truncated traces list that will not crash the Binder or whatever.
     */
    protected fun braveTruncateAsNeeded(stackTraces: Array<String>): MutableList<String> {
        val limit = 104857 // limit to around 100k

        var size = 0
        val finalList: MutableList<String> = ArrayList()

        for (trace in stackTraces) {
            if (limit < size) {
                finalList.add("BravePipe TRUNCATED trace")
                break
            }
            size += trace.length
            finalList.add(trace)
        }
        return finalList
    }

    protected fun braveAddLogcatLogAttachmentToMail(
        context: Context,
        emailIntent: Intent,
        errorInfo: ErrorInfo
    ): Intent {
        if (!isLogcatDumperEnabled(context)) return emailIntent

        val uris = ArrayList<Uri?>()

        val possibleLogcatLogFile = getPossibleLogFileBasedOnTimestamp(context, errorInfo)
        possibleLogcatLogFile?.let {
            val uri = FileProvider.getUriForFile(
                context,
                LogcatFileProvider.getAuthority(context),
                possibleLogcatLogFile
            )
            uris.add(uri)
        }

        if (!uris.isEmpty()) {
            emailIntent.apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)

                // val clipData = ClipData.newRawUri("Logs", uris.get(0))
                // for (i in 1..<uris.size) {
                //    clipData.addItem(ClipData.Item(uris.get(i)))
                // }
                // emailIntent.clipData = clipData
            }
        }

        return emailIntent
    }

    /**
     * Create the timestamp based on the time the ErrorInfo was first created.
     *
     * @param currentTimeStamp in case we have no own timestamp we give this one back to the caller
     */
    protected fun braveGetErrorCreationTimestamp(
        currentTimeStamp: String,
        errorInfo: ErrorInfo
    ): String {
        if (!isLogcatDumperEnabled()) return currentTimeStamp

        val dateTime = Instant.ofEpochMilli(errorInfo.braveErrorInfoCreationTimestamp)
            .atZone(ZoneId.systemDefault())
        return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    protected fun braveAddCopyLogcatLogButton(
        activity: AppCompatActivity,
        copyButton: Button,
        errorInfo: ErrorInfo
    ) {
        if (!isLogcatDumperEnabled(activity)) return

        val parentLayout = copyButton.parent as? ViewGroup ?: return
        val context = copyButton.context

        val newCopyLogcatButton = Button(context).apply {
            text = "Copy related logcat entries"
            id = View.generateViewId() // important for accessibility and constraints
        }

        // copy layout also to have same look and feel
        val params = copyButton.layoutParams
        newCopyLogcatButton.layoutParams = params

        if (parentLayout is LinearLayout) {
            val index = parentLayout.indexOfChild(copyButton)
            parentLayout.addView(newCopyLogcatButton, index + 1)
        }

        newCopyLogcatButton.setOnClickListener {
            activity.lifecycleScope.launch {
                try {
                    val fileContent = withContext(Dispatchers.IO) {
                        val possibleLogcatLogFile =
                            getPossibleLogFileBasedOnTimestamp(activity, errorInfo)
                        possibleLogcatLogFile?.readText(Charsets.UTF_8)
                    }

                    if (null != fileContent) {
                        ShareUtils.copyToClipboard(context, fileContent)
                    } else {
                        Toast.makeText(context, "no logcat log available", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "error reading logcat file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getPossibleLogFileBasedOnTimestamp(
        context: Context,
        errorInfo: ErrorInfo
    ): File? {
        val logFileNameCreator = BraveLogFileNameErrorInfo()
        logFileNameCreator.setTimestamp(errorInfo.braveErrorInfoCreationTimestamp)
        // we assume that while the app was crashing a log file with
        // that name was created
        val possibleLogFileBasedOnTimestamp = logFileNameCreator.getLogFileName()

        val logDir = logcatDump.getLogFolder(context)
        val file = File(logDir, possibleLogFileBasedOnTimestamp)
        if (file.exists()) {
            return file
        }
        return null
    }
}
