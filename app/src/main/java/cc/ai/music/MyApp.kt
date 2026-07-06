package cc.ai.music

import android.app.Application
import android.app.Application.getProcessName
import android.os.Bundle
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.PowerManager
import android.view.Display
import androidx.work.Configuration
import androidx.work.WorkManager

class MyApp : Application() {
    private var displayListener: DisplayManager.DisplayListener? = null
    private var lastDisplayState: Int = Display.STATE_UNKNOWN
    private var screenOnReceiver: BroadcastReceiver? = null
    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        val currentProcessName = getProcessName()
        val isMainProcess = currentProcessName == packageName
        WorkDebugRecorder.record(
            this,
            "MY_APP_CREATE",
            "package=$packageName, processName=$currentProcessName, isMainProcess=$isMainProcess"
        )
        if (!isMainProcess) {
            WorkDebugRecorder.record(
                this,
                "MY_APP_SKIP_NON_MAIN_PROCESS",
                "package=$packageName, processName=$currentProcessName"
            )
            return
        }
        val configuration = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManager.initialize(this, configuration)
        WorkDebugRecorder.record(this, "WORKMANAGER_INITIALIZED", "logLevel=DEBUG")
        registerAppForegroundBackgroundObserver()
        registerScreenOnListeners()
    }

    private fun registerAppForegroundBackgroundObserver() {
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: android.app.Activity) {
                    val wasInBackground = startedActivityCount == 0
                    startedActivityCount += 1
                    if (wasInBackground) {
                        WorkDebugRecorder.record(this@MyApp, "APP_MOVED_TO_FOREGROUND")
                    }
                }

                override fun onActivityStopped(activity: android.app.Activity) {
                    startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
                    if (startedActivityCount == 0) {
                        WorkDebugRecorder.record(this@MyApp, "APP_MOVED_TO_BACKGROUND")
                    }
                }

                override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityResumed(activity: android.app.Activity) = Unit

                override fun onActivityPaused(activity: android.app.Activity) = Unit

                override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: android.app.Activity) = Unit
            }
        )
    }

    private fun registerScreenOnListeners() {
        registerDisplayListener()
        registerScreenOnReceiver()
    }

    private fun registerDisplayListener() {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        lastDisplayState = if (powerManager?.isInteractive == true) Display.STATE_ON else Display.STATE_OFF
        WorkDebugRecorder.record(
            this,
            "SCREEN_ON_REGISTERED",
            "DISPLAY",
            "lastDisplayState=${displayStateText(lastDisplayState)}"
        )
        displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit

            override fun onDisplayRemoved(displayId: Int) = Unit

            override fun onDisplayChanged(displayId: Int) {
                if (displayId != Display.DEFAULT_DISPLAY) return
                val display = displayManager.getDisplay(displayId) ?: return
                val state = display.state
                if (lastDisplayState != Display.STATE_ON && state == Display.STATE_ON) {
                    WorkDebugRecorder.record(
                        this@MyApp,
                        "SCREEN_ON_EVENT",
                        "DISPLAY",
                        "fromState=${displayStateText(lastDisplayState)}, toState=${displayStateText(state)}"
                    )
                }
                lastDisplayState = state
            }
        }
        displayManager.registerDisplayListener(displayListener, null)
    }

    private fun registerScreenOnReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_ON) {
                    WorkDebugRecorder.record(
                        this@MyApp,
                        "SCREEN_ON_EVENT",
                        "BROADCAST",
                        "action=${intent.action}"
                    )
                }
            }
        }
        screenOnReceiver = receiver
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        WorkDebugRecorder.record(this, "SCREEN_ON_REGISTERED", "BROADCAST", "action=ACTION_SCREEN_ON")
    }

    private fun displayStateText(state: Int): String {
        return when (state) {
            Display.STATE_OFF -> "OFF"
            Display.STATE_ON -> "ON"
            Display.STATE_DOZE -> "DOZE"
            Display.STATE_DOZE_SUSPEND -> "DOZE_SUSPEND"
            Display.STATE_VR -> "VR"
            Display.STATE_ON_SUSPEND -> "ON_SUSPEND"
            Display.STATE_UNKNOWN -> "UNKNOWN"
            else -> "STATE_$state"
        }
    }
}
