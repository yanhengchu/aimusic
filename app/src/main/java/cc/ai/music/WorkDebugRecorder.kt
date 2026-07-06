package cc.ai.music

import android.app.ActivityManager
import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WorkDebugRecorder {
    const val EXTRA_COMMAND = "workmanager_command"
    const val COMMAND_CREATE_PERIODIC_WORK = "create_periodic_work"
    const val COMMAND_QUERY_PERIODIC_WORK = "query_periodic_work"
    const val COMMAND_CANCEL_PERIODIC_WORK = "cancel_periodic_work"

    private const val TRACE_FILE_NAME = "workmanager_trace.txt"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    @Synchronized
    fun record(context: Context, event: String, detail: String = "") {
        writeTraceLine(context, event, null, detail)
    }

    @Synchronized
    fun record(context: Context, event: String, phase: String, detail: String = "") {
        writeTraceLine(context, event, phase, detail)
    }

    private fun writeTraceLine(context: Context, event: String, phase: String?, detail: String) {
        val now = System.currentTimeMillis()
        val line = buildString {
            append(dateFormat.format(Date(now)))
            append("|")
            append(event)
            if (phase != null) {
                append("|")
                append(phase)
            }
            append("|")
            append(detail)
            append("\n")
        }
        traceFile(context).appendText(line)
    }

    @Synchronized
    fun clear(context: Context) {
        traceFile(context).delete()
    }

    fun traceFile(context: Context): File = File(context.filesDir, TRACE_FILE_NAME)

    fun readTrace(context: Context): String {
        val file = traceFile(context)
        return if (file.exists()) file.readText() else ""
    }

    fun currentAppRunState(context: Context): String {
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return "unknown"
        val myPid = android.os.Process.myPid()
        val processInfo = activityManager.runningAppProcesses?.firstOrNull { it.pid == myPid } ?: return "unknown"
        return when (processInfo.importance) {
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND,
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "foreground"
            else -> "background"
        }
    }
}
