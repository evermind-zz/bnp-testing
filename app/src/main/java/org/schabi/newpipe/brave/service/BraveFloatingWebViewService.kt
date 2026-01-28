package org.schabi.newpipe.brave.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.schabi.newpipe.brave.bus.BraveBus
import org.schabi.newpipe.brave.misc.BraveRumbleCloudflareEvents
import org.schabi.newpipe.brave.misc.BraveRumbleCloudflareWebViewHandler
import org.schabi.newpipe.util.DeviceUtils.dpToPx

class BraveFloatingWebViewService :
    Service(),
    BraveRumbleCloudflareEvents.EventServiceActions.Handler,
    BraveRumbleCloudflareEvents.EventCloudflareChallengeRequest.Handler {

    private var closeZoneAdded = false
    private var webViewAdded = false
    private lateinit var windowManager: WindowManager
    private lateinit var webView: WebView
    private lateinit var overlayParams: WindowManager.LayoutParams

    private lateinit var handler: BraveRumbleCloudflareWebViewHandler

    private lateinit var closeZoneView: View
    private lateinit var closeZoneParams: WindowManager.LayoutParams

    private val IDLE_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes

    private val idleHandler = Handler(Looper.getMainLooper())
    private val idleRunnable = Runnable {
        closeOverlay()
    }

    private fun resetIdleTimer() {
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, IDLE_TIMEOUT_MS)
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING,
                TelephonyManager.CALL_STATE_OFFHOOK -> pauseWebView()

                TelephonyManager.CALL_STATE_IDLE -> resumeWebView()
            }
        }
    }

    private fun pauseWebView() {
        webView.onPause()
        webView.pauseTimers()
        idleHandler.removeCallbacks(idleRunnable)
    }

    private fun resumeWebView() {
        webView.onResume()
        webView.resumeTimers()
        resetIdleTimer()
    }

    override fun onCreate() {
        super.onCreate()
        resetIdleTimer()

        startForeground(NOTIFICATION_ID, createNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
        createCloseZoneFlowingWindow()

        val telephonyManager =
            getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_CALL_STATE
        )

        BraveBus.getBus().register(this)
        BraveBus.getBus().post(BraveRumbleCloudflareEvents.EventCloudflareServiceReady())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun safelyRemoveWebView() {
        if (::webView.isInitialized &&
            webViewAdded &&
            webView.isAttachedToWindow
        ) {
            try {
                windowManager.removeView(webView)
            } catch (e: IllegalArgumentException) {
                print(e)
                // OEM or race-condition safety
            } finally {
                webViewAdded = false
            }
        }
    }

    override fun onDestroy() {
        idleHandler.removeCallbacks(idleRunnable)

        handler.destroy()
        BraveBus.getBus().unregister(this)

        safelyRemoveWebView()
        if (::webView.isInitialized) {
            webView.destroy()
        }

        safelyRemoveCloseZoneView()

        super.onDestroy()
    }

    private fun safelyRemoveCloseZoneView() {
        if (::closeZoneView.isInitialized &&
            closeZoneAdded &&
            closeZoneView.isAttachedToWindow
        ) {
            try {
                windowManager.removeView(closeZoneView)
            } catch (e: IllegalArgumentException) {
                print(e)
                // OEM or race-condition safety
            } finally {
                closeZoneAdded = false
            }
        }
    }

    // ----------------------------------------------------
    // Overlay creation
    // ----------------------------------------------------

    @SuppressLint("SetJavaScriptEnabled")
    private fun createOverlay() {

        overlayParams = WindowManager.LayoutParams(
            1, // start minimized
            1,
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        windowManager.addView(webView, overlayParams)
        webViewAdded = true

        setWebViewTouchListener(webView)

        handler = BraveRumbleCloudflareWebViewHandler(webView)
    }

    // Add this method in your service or activity class
    private fun setWebViewTouchListener(webView: WebView) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var isDragging = false
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        webView.setOnTouchListener { v, event ->
            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    // Record initial touch positions
                    startX = overlayParams.x
                    startY = overlayParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    isDragging = false

                    // Let WebView handle the DOWN action (so clicks still work)
                    false
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY

                    // Check if the movement is greater than touch slop (start dragging)
                    if (!isDragging && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                        showCloseZone()
                        isDragging = true
                        v.parent?.requestDisallowInterceptTouchEvent(true) // Prevent parent from intercepting
                    }

                    if (isDragging) {
                        // Move the overlay window with the drag
                        overlayParams.x = startX + dx.toInt()
                        overlayParams.y = startY + dy.toInt()
                        windowManager.updateViewLayout(webView, overlayParams)
                        true
                    } else {
                        false
                    }
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    hideCloseZone()

                    // Handle the drop logic, close or snap
                    if (isDragging) {
                        if (isInCloseZone(event.rawY)) {
                            closeOverlay()
                        } else {
                            snapToEdge()
                        }
                        isDragging = false
                        true
                    } else {
                        // Let WebView handle the click (for tap/click interaction)
                        false
                    }
                }

                else -> false
            }
        }
    }

    private fun closeOverlay() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun showCloseZone() {
        if (!closeZoneView.isAttachedToWindow && !closeZoneAdded) {
            windowManager.addView(closeZoneView, closeZoneParams)
            closeZoneAdded = true
        }
    }

    private fun hideCloseZone() {
        safelyRemoveCloseZoneView()
    }

    private fun snapToEdge() {
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        overlayParams.x =
            if (overlayParams.x + overlayParams.width / 2 < screenWidth / 2)
                0
            else
                screenWidth - overlayParams.width

        windowManager.updateViewLayout(webView, overlayParams)
    }

    private fun snapToEdge2() { // use that if  problems on sdk 19
        val dm = Resources.getSystem().displayMetrics
        val screenWidth = dm.widthPixels

        overlayParams.x =
            if (overlayParams.x + overlayParams.width / 2 < screenWidth / 2) {
                0
            } else {
                screenWidth - overlayParams.width
            }

        windowManager.updateViewLayout(webView, overlayParams)
    }

    // Close zone hit test (TOP zone)
    private fun isInCloseZone(rawY: Float): Boolean {
        return rawY <= dpToPx(80, baseContext)
    }

    private fun getWindowType(): Int {
        val windowType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
        return windowType
    }

    private fun createCloseZoneFlowingWindow() {
        closeZoneView = FrameLayout(this).apply {
            setBackgroundColor(0x88FF0000.toInt()) // semi-transparent red
        }

        closeZoneParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dpToPx(80, baseContext),
            getWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        // NOT added yet — added only while dragging
    }

    // ----------------------------------------------------
    // Public control methods (call via Intent / Binder)
    // ----------------------------------------------------

    fun minimize() {
        overlayParams.width = 1
        overlayParams.height = 1
        overlayParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        windowManager.updateViewLayout(webView, overlayParams)
    }

    fun expandInteractive() {
        val dm = Resources.getSystem().displayMetrics

        overlayParams.width = (dm.widthPixels * 0.75).toInt()
        overlayParams.height = (dm.heightPixels * 0.75).toInt()
        // overlayParams.flags = 0 // allow focus + touch
        overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        windowManager.updateViewLayout(webView, overlayParams)

        webView.setBackgroundColor(Color.WHITE)
    }

    fun expandNonInteractive() {
        val dm = Resources.getSystem().displayMetrics

        overlayParams.width = (dm.widthPixels * 0.75).toInt()
        overlayParams.height = (dm.heightPixels * 0.75).toInt()
        overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        windowManager.updateViewLayout(webView, overlayParams)
    }

    // ----------------------------------------------------
    // Foreground notification
    // ----------------------------------------------------
    private fun createNotification(): Notification {
        val channelId = "floating_webview"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating WebView",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("WebView active")
            .setContentText("Background web content running")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }

    // /section handle the eventbus
    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun handleEventServiceActions(event: BraveRumbleCloudflareEvents.EventServiceActions) {
        resetIdleTimer()
        when (event.action) {
            BraveRumbleCloudflareEvents.EventServiceActions.Actions.ShutdownService -> closeOverlay()
            BraveRumbleCloudflareEvents.EventServiceActions.Actions.MinimizeOverlay -> minimize()
            BraveRumbleCloudflareEvents.EventServiceActions.Actions.InteractiveOverlay -> expandInteractive()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun handleEventFetchContentRequest(event: BraveRumbleCloudflareEvents.EventCloudflareChallengeRequest) {
        resetIdleTimer()
        handler.fetchContentViaWebView(event.url, event.timeoutMs)
    }
}
