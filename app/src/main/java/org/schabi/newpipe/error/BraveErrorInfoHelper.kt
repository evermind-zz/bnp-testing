package org.schabi.newpipe.error

import android.util.Log
import org.schabi.newpipe.brave.feature.logcat.BraveLogcatDumper
import org.schabi.newpipe.error.ErrorInfo.Companion.throwableListToStringList
import org.schabi.newpipe.error.ErrorInfo.Companion.throwableToStringList
import org.schabi.newpipe.extractor.brave.AttachException

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
        logIfBraveLocatDumperIsEnabled(stackTraces)
        return stackTraces
    }

    fun logStackTraces(throwable: Throwable): Array<String> {
        val stackTraces: Array<String> = throwableToStringList(throwable)
        logDataIfAttachException(throwable)
        logIfBraveLocatDumperIsEnabled(stackTraces)
        return stackTraces
    }

    fun logStackTraces(throwables: List<Throwable>): Array<String> {
        val stackTraces: Array<String> = throwableListToStringList(throwables)
        throwables.forEach { throwable ->
            logDataIfAttachException(throwable)
        }
        logIfBraveLocatDumperIsEnabled(stackTraces)
        return stackTraces
    }

    /**
     * if the exception is [AttachException] we dump its user given data to the Logger.
     */
    private fun logDataIfAttachException(throwable: Throwable) {
        if (throwable is AttachException) {
            throwable.exceptionData.forEach { data ->
                val stackTrace = throwable.stackTrace
                if (stackTrace.isNotEmpty()) {
                    val element = stackTrace[0]
                    val className = element.className.substringAfterLast(".")
                    Log.e(
                        "AttachExceptionData",
                        "[${className}.${element.methodName}() line:${element.lineNumber}] DATA: $data"
                    )
                } else {
                    Log.e(
                        "AttachExceptionData",
                        "DATA: $data"
                    )
                }
            }
        }
    }

    private fun logIfBraveLocatDumperIsEnabled(stackTraces: Array<String>): Array<String> {
        if (!BraveLogcatDumper.isLogcatDumperEnabled()) return stackTraces

        // print stack trace once again for debugging:
        var count = 0 // how many traces do we have
        stackTraces.forEach {
            Log.e("${count++}_${BraveLogcatDumper::class.simpleName}", it)
        }

        return stackTraces
    }
}
