package org.schabi.newpipe.error

import androidx.appcompat.app.AppCompatActivity

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
}
