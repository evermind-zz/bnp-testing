package coil3

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap

class FakeCoilImageBitmap(private val bitmap: Bitmap?) : Image {
    override fun toBitmap(): Bitmap? = bitmap
}

class FakeCoilImageDrawable(private val drawable: Drawable?) : Image {
    override fun toBitmap(): Bitmap? = drawable?.toBitmap()
}
