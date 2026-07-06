package cc.ai.music

import android.content.Context
import android.app.job.JobScheduler
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PeriodicLogWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val workId = id.toString()
        val appRunState = WorkDebugRecorder.currentAppRunState(applicationContext)
        val startedAt = System.currentTimeMillis()
        val startedAtText = DATE_FORMAT.format(Date(startedAt))
        WorkDebugRecorder.record(
            applicationContext,
            "PERIODIC_WORK_EXECUTE",
            "START",
            "workId=$workId, runAttemptCount=$runAttemptCount, startedAt=$startedAtText, appRunState=$appRunState"
        )
        WorkDebugRecorder.record(
            applicationContext,
            "JOBSCHEDULER_EXECUTE",
            "SNAPSHOT",
            buildJobSchedulerExecuteSnapshot()
        )
        return try {
            val executedAt = System.currentTimeMillis()
            val executedAtText = DATE_FORMAT.format(Date(executedAt))
            applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_EXECUTED_AT, executedAt)
                .putString(KEY_LAST_EXECUTED_AT_TEXT, executedAtText)
                .apply()
            val suggestedNextCheckTime = executedAt + TimeUnit.MINUTES.toMillis(INTERVAL_MINUTES + 1)
            WorkDebugRecorder.record(
                applicationContext,
                "PERIODIC_WORK_EXECUTE",
                "RESULT",
                "workId=$workId, runAttemptCount=$runAttemptCount, result=success, executedAt=$executedAtText, appRunState=$appRunState, durationMs=${executedAt - startedAt}, stopped=$isStopped"
            )
            WorkDebugRecorder.record(
                applicationContext,
                "PERIODIC_WORK_EXECUTE",
                "NEXT_CHECK_HINT",
                "workId=$workId, suggestedCheckAfter=${DATE_FORMAT.format(Date(suggestedNextCheckTime))}, intervalMinutes=$INTERVAL_MINUTES"
            )
            Log.d(TAG, "PeriodicLogWorker executed at $executedAtText, workId=$workId")
            Result.success()
        } catch (throwable: Throwable) {
            WorkDebugRecorder.record(
                applicationContext,
                "PERIODIC_WORK_EXECUTE",
                "ERROR",
                "workId=$workId, runAttemptCount=$runAttemptCount, error=${throwable.javaClass.simpleName}, message=${throwable.message ?: "none"}"
            )
            throw throwable
        }
    }

    override fun onStopped() {
        super.onStopped()
        WorkDebugRecorder.record(
            applicationContext,
            "PERIODIC_WORK_EXECUTE",
            "STOPPED",
            "workId=${id}, runAttemptCount=$runAttemptCount, appRunState=${WorkDebugRecorder.currentAppRunState(applicationContext)}, stopped=$isStopped"
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "periodic_log_worker"
        const val INTERVAL_MINUTES = 15L
        const val INITIAL_DELAY_SECONDS = 5L
        const val SYSTEM_JOB_SERVICE_CLASS_NAME =
            "androidx.work.impl.background.systemjob.SystemJobService"

        private const val TAG = "PeriodicLogWorker"
        private const val PREFS_NAME = "workmanager_debug"
        private const val KEY_LAST_EXECUTED_AT = "last_executed_at"
        private const val KEY_LAST_EXECUTED_AT_TEXT = "last_executed_at_text"
        private const val KEY_ENQUEUE_REQUESTED_AT = "enqueue_requested_at"
        private const val KEY_ENQUEUE_REQUESTED_AT_TEXT = "enqueue_requested_at_text"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        fun clearLastExecuted(context: Context) {
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_LAST_EXECUTED_AT)
                .remove(KEY_LAST_EXECUTED_AT_TEXT)
                .apply()
        }

        fun recordEnqueueRequestedAt(context: Context, enqueueRequestedAt: Long) {
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_ENQUEUE_REQUESTED_AT, enqueueRequestedAt)
                .putString(KEY_ENQUEUE_REQUESTED_AT_TEXT, DATE_FORMAT.format(Date(enqueueRequestedAt)))
                .apply()
        }

        fun clearEnqueueRequestedAt(context: Context) {
            context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ENQUEUE_REQUESTED_AT)
                .remove(KEY_ENQUEUE_REQUESTED_AT_TEXT)
                .apply()
        }

        fun readLastExecutedAtText(context: Context): String {
            val value = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_EXECUTED_AT_TEXT, null)
            return value ?: "never"
        }

        fun readLastExecutedAtMillis(context: Context): Long? {
            val value = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_EXECUTED_AT, -1L)
            return value.takeIf { it > 0L }
        }

        fun readEnqueueRequestedAtMillis(context: Context): Long? {
            val value = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_ENQUEUE_REQUESTED_AT, -1L)
            return value.takeIf { it > 0L }
        }
    }

    private fun buildJobSchedulerExecuteSnapshot(): String {
        val jobScheduler = applicationContext.getSystemService(JobScheduler::class.java)
        val trackedJobs = jobScheduler
            ?.allPendingJobs
            .orEmpty()
            .filter { job ->
                job.service.packageName == applicationContext.packageName &&
                    job.service.className == SYSTEM_JOB_SERVICE_CLASS_NAME
            }
        val activeJob = trackedJobs.firstOrNull()
        return "jobCount=${trackedJobs.size}, jobId=${activeJob?.id ?: "none"}, service=${activeJob?.service?.flattenToShortString() ?: "none"}, isPeriodic=${activeJob?.isPeriodic ?: false}, isPersisted=${activeJob?.isPersisted ?: false}, requiresCharging=${activeJob?.isRequireCharging ?: false}, requiresDeviceIdle=${activeJob?.isRequireDeviceIdle ?: false}"
    }
}
