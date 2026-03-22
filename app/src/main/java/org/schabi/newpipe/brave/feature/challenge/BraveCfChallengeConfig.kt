package org.schabi.newpipe.brave.feature.challenge

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import com.github.evermindzz.challengefloatsaway.ChallengeSettings
import com.github.evermindzz.challengefloatsaway.ui.SimpleDialogProvider
import org.schabi.newpipe.DownloaderImpl
import org.schabi.newpipe.R
import org.schabi.newpipe.util.ThemeHelper

object BraveCfChallengeConfig {

    fun updateFloatingVisible(isInteractive: Boolean) =
        ChallengeSettings.update { it.copy(isInteractive = isInteractive) }

    fun init(isInteractive: Boolean) {
        ChallengeSettings.update { current ->
            current.copy(
                userAgent = DownloaderImpl.USER_AGENT,
                cookieDomains = arrayOf<String>(
                    "https://rumble.com",
                    "rumble.com",
                    ".rumble.com",
                    "https://www.rumble.com",
                    "www.rumble.com"
                ),
                isInteractive = isInteractive,
                dialogProvider = BraveDialogProvider()
            )
        }
    }
}

class BraveDialogProvider : SimpleDialogProvider {

    override fun showConfirmDialog(
        context: Context,
        title: String?,
        message: CharSequence,
        positive: String,
        negative: String?,
        onPositive: () -> Unit,
        onNegative: () -> Unit
    ) {
        val themeWrapperContext = getThemeWrapperContext(context)
        AlertDialog.Builder(themeWrapperContext)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positive) { _, _ -> onPositive() }
            .apply {
                if (negative != null) {
                    setNegativeButton(negative) { _, _ -> onNegative() }
                } else {
                    setNegativeButton(null as String?, null)
                }
            }
            .setOnCancelListener { onNegative() }
            .show()
    }

    private fun getThemeWrapperContext(context: Context): ContextThemeWrapper {
        val themeResId = if (ThemeHelper.isLightThemeSelected(context)) {
            R.style.LightTheme
        } else {
            R.style.DarkTheme
        }
        return ContextThemeWrapper(context, themeResId)
    }
}
