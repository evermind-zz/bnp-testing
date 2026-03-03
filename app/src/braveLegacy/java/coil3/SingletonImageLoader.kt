package coil3

import android.content.Context

object SingletonImageLoader {

    fun set(factory: Factory) {
        // in legacy flavor: ignore we do not need a real factory
    }

    @JvmStatic
    fun get(context: Context): ImageLoader {
        return ImageLoader.Builder(context).build()
    }

    fun Factory(context: Context): Factory {
        return object : Factory {
            override fun newImageLoader(context: Context): ImageLoader {
                return ImageLoader.Builder(context).build()
            }
        }
    }

    interface Factory {
        fun newImageLoader(context: Context): ImageLoader
    }
}
