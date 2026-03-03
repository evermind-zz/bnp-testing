package coil3.bravePipeLegacy

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import coil3.FakeCoilImageBitmap
import coil3.FakeCoilImageDrawable
import coil3.target.Target
import com.squareup.picasso.Picasso

class PicassoTargetWrapper(
    private val coilTarget: Target
) : com.squareup.picasso.Target {

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        // Coil does not know "from" we simulate success with the bitmap
        coilTarget.onSuccess(bitmap?.let { FakeCoilImageBitmap(it) } ?: FakeCoilImageBitmap(null))
    }

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
        coilTarget.onError(FakeCoilImageDrawable(errorDrawable))
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        coilTarget.onStart(FakeCoilImageDrawable(placeHolderDrawable))
    }
}