package org.schabi.newpipe.util.image

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil3.bravePipeLegacy.PicassoHelper
import coil3.bravePipeLegacy.PicassoTargetWrapper
import coil3.request.Disposable
import coil3.target.Target
import org.schabi.newpipe.R
import org.schabi.newpipe.extractor.Image
import java.io.IOException

/**
 * delegate all Coil calls to Picasso for braveLegacy flavor.
 *
 * See BravePipeLegacy's fake [coil3] API to let us use save load of
 * maintaining nightmare -- hopefully:).
 */
object CoilHelper {

    private val TAG = CoilHelper::class.java.simpleName

    @JvmOverloads
    fun loadBitmapBlocking(
        context: Context,
        url: String?,
        @DrawableRes placeholderResId: Int = 0
    ): Bitmap? {
        if (url.isNullOrEmpty()) return null
        val creator = when (placeholderResId) {
            R.drawable.ic_newpipe_triangle_white -> PicassoHelper.loadNotificationIcon(url)
            else -> PicassoHelper.loadSeekbarThumbnailPreview(url)
        }
        return try {
            creator.get()
        } catch (e: IOException) {
            Log.e(TAG, "loadBitmapBlocking failed for $url", e)
            null
        }
    }

    fun loadAvatar(target: ImageView, images: List<Image>) =
        PicassoHelper.loadAvatar(images).into(target)

    fun loadAvatar(target: ImageView, url: String?) =
        PicassoHelper.loadAvatar(url).into(target)

    fun loadThumbnail(target: ImageView, images: List<Image>) =
        PicassoHelper.loadThumbnail(images).into(target)

    fun loadThumbnail(target: ImageView, url: String?) =
        PicassoHelper.loadThumbnail(url).into(target)

    fun loadDetailsThumbnail(target: ImageView, images: List<Image>) =
        PicassoHelper.loadDetailsThumbnail(images).into(target)

    fun loadBanner(target: ImageView, images: List<Image>) =
        PicassoHelper.loadBanner(images).into(target)

    fun loadPlaylistThumbnail(target: ImageView, images: List<Image>) =
        PicassoHelper.loadPlaylistThumbnail(images).into(target)

    fun loadPlaylistThumbnail(target: ImageView, url: String?) =
        PicassoHelper.loadPlaylistThumbnail(url).into(target)

    fun loadScaledDownThumbnail(
        context: Context,
        images: List<Image>,
        target: Target
    ): Disposable {
        val picassoTargetWrapper = PicassoTargetWrapper(target)
        val tag = "legacy_player_thumbnail_${System.identityHashCode(target)}"  // unique per call

        PicassoHelper.loadScaledDownThumbnail(context, images)
            .tag(tag)
            .into(picassoTargetWrapper)

        return Disposable { PicassoHelper.cancelTag(tag) }
    }

    @JvmStatic
    fun dispose(imageView: ImageView) = PicassoHelper.cancelRequest(imageView)
}
