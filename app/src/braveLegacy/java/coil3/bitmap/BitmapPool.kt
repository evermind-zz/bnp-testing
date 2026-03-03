package coil3.bitmap

import android.graphics.Bitmap
import androidx.annotation.Px

interface BitmapPool {

    companion object {
        @JvmStatic
        @JvmName("create")
        operator fun invoke(maxSize: Int): BitmapPool {
            return object : BitmapPool {
                override fun put(bitmap: Bitmap) {
                    TODO("if the app crashes report this")
                }

                override fun get(
                    width: Int,
                    height: Int,
                    config: Bitmap.Config
                ): Bitmap {
                    TODO("if the app crashes report this")
                }

                override fun getOrNull(
                    width: Int,
                    height: Int,
                    config: Bitmap.Config
                ): Bitmap? {
                    TODO("if the app crashes report this")
                }

                override fun getDirty(
                    width: Int,
                    height: Int,
                    config: Bitmap.Config
                ): Bitmap {
                    TODO("if the app crashes report this")
                }

                override fun getDirtyOrNull(
                    width: Int,
                    height: Int,
                    config: Bitmap.Config
                ): Bitmap? {
                    TODO("if the app crashes report this")
                }

                override fun trimMemory(level: Int) {
                    TODO("if the app crashes report this")
                }

                override fun clear() {
                    TODO("if the app crashes report this")
                }

            }
        }
    }

    fun put(bitmap: Bitmap)
    fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap
    fun getOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?
    fun getDirty(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap
    fun getDirtyOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?
    fun trimMemory(level: Int)
    fun clear()
}
