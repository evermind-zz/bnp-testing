package coil3.transform

import android.graphics.Bitmap
import coil3.bitmap.BitmapPool
import coil3.size.Size

interface Transformation {

    fun key(): String

    suspend fun transform(pool: BitmapPool, input: Bitmap, size: Size): Bitmap
}
