package org.schabi.newpipe.error

import android.util.Log
import org.schabi.newpipe.brave.feature.logcat.BraveLogcatDumper

object BraveErrorInfoHelper {

    /**
     * dump [ErrorInfo] stack traces via Log.e.
     *
     * - The traces need to be dumped early for [BraveLogcatDumper] to actually catch
     *   them. In the current NewPipe implementation the traces will be dumped
     *   to logcat only if the [ErrorActivity] is already started. That is way too late and
     *   also user dependent.
     * - If the [BraveLogcatDumper] is enabled we disable the dumping in [ErrorActivity]
     */
    fun logStackTraces(stackTraces: Array<String>): Array<String> {
        if (!BraveLogcatDumper.isLogcatDumperEnabled()) return stackTraces

        // print stack trace once again for debugging:
        var count = 0 // how many traces do we have
        stackTraces.forEach {
            Log.e("${count++}_${BraveLogcatDumper::class.simpleName}", it)
        }

        return stackTraces
    }
}
