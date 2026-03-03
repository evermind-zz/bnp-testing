package coil3

import android.content.Context
import coil3.disk.DiskCache
import coil3.memory.MemoryCache

class ImageLoader private constructor() {

    fun getMemoryCache(): MemoryCache = MemoryCache()
    fun getDiskCache(): DiskCache = DiskCache()


    class Builder(context: Context) {
        fun logger(logger: Any?): Builder = this

        fun components(block: ComponentRegistry.Builder.() -> Unit): Builder {
            return this
        }

        fun build(): ImageLoader = ImageLoader()
    }
}
