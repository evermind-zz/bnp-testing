package org.schabi.newpipe.brave.tip

import android.os.Parcelable
import java.io.Serializable
import kotlinx.parcelize.Parcelize

@Parcelize
class BraveTipComponent
@JvmOverloads
constructor(
    val name: String,
    val isCopyable: Boolean,
    val link: String? = null,
    val address: String? = null
) : Parcelable, Serializable
