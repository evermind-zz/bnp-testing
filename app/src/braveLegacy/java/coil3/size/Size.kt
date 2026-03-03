package coil3.size

import android.os.Parcelable
import androidx.annotation.Px
import kotlinx.parcelize.Parcelize

sealed class Size : Parcelable

@Parcelize
object OriginalSize : Size() {
    override fun toString() = "coil3.size.OriginalSize"
}

@Parcelize
data class PixelSize(
    @Px val width: Int,
    @Px val height: Int
) : Size() {

    init {
        require(width > 0 && height > 0) { "width and height must be > 0." }
    }
}
