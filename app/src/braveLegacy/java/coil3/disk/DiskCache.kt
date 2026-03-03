package coil3.disk

class DiskCache {
    class SnapShot : AutoCloseable {
        fun getData(): String = ""

        override fun close() {}
    }

    fun clear() {}

    fun openSnapshot(thumbnailUrl: String): SnapShot {
        return SnapShot()
    }
}
