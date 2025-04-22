package org.schabi.newpipe.brave.tip

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
class BraveTipComponent
@JvmOverloads
constructor(
    val name: String,
    val isCopyable: Boolean,
    val link: String? = null,
    val address: String? = null
) : Parcelable, Serializable
