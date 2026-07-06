package cc.ai.music

import android.app.job.JobScheduler
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var stageText: TextView
    private lateinit var stageDescriptionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workmanager)
        stageText = findViewById(R.id.stageText)
        stageDescriptionText = findViewById(R.id.stageDescriptionText)
        updateStageUi(intent)
        WorkDebugRecorder.record(this, "MAIN_ACTIVITY_CREATE", buildStartupDetail(intent))
        handleStartupCommand(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateStageUi(intent)
        WorkDebugRecorder.record(this, "MAIN_ACTIVITY_NEW_INTENT", buildStartupDetail(intent))
        handleStartupCommand(intent)
    }

    private fun enqueuePeriodicWork() {
        val intervalMinutes = PeriodicLogWorker.INTERVAL_MINUTES
        val initialDelaySeconds = PeriodicLogWorker.INITIAL_DELAY_SECONDS
        val request = PeriodicWorkRequestBuilder<PeriodicLogWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
            .build()
        val workManager = WorkManager.getInstance(this)
        val enqueueRequestedAt = System.currentTimeMillis()
        WorkDebugRecorder.record(this, "PERIODIC_WORK_ENQUEUE", "START", "policy=UPDATE, uniqueName=${PeriodicLogWorker.UNIQUE_WORK_NAME}")
        workManager.enqueueUniquePeriodicWork(
            PeriodicLogWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        PeriodicLogWorker.recordEnqueueRequestedAt(this, enqueueRequestedAt)
        WorkDebugRecorder.record(
            this,
            "PERIODIC_WORK_ENQUEUE",
            "RESULT",
            "requestId=${request.id}, uniqueName=${PeriodicLogWorker.UNIQUE_WORK_NAME}, intervalMinutes=$intervalMinutes, initialDelaySeconds=$initialDelaySeconds, policy=UPDATE(single active work)"
        )
        val suggestedCheckTime = enqueueRequestedAt + TimeUnit.SECONDS.toMillis(initialDelaySeconds)
        WorkDebugRecorder.record(
            this,
            "PERIODIC_WORK_ENQUEUE",
            "FIRST_CHECK_HINT",
            "requestId=${request.id}, suggestedCheckAfter=${TRACE_DATE_FORMAT.format(Date(suggestedCheckTime))}, initialDelaySeconds=$initialDelaySeconds"
        )
        refreshStatus()
    }

    private fun cancelPeriodicWork() {
        val workManager = WorkManager.getInstance(this)
        WorkDebugRecorder.record(this, "PERIODIC_WORK_CANCEL", "START", "uniqueName=${PeriodicLogWorker.UNIQUE_WORK_NAME}")
        workManager.cancelUniqueWork(PeriodicLogWorker.UNIQUE_WORK_NAME)
        PeriodicLogWorker.clearEnqueueRequestedAt(this)
        WorkDebugRecorder.record(
            this,
            "PERIODIC_WORK_CANCEL",
            "RESULT",
            "uniqueName=${PeriodicLogWorker.UNIQUE_WORK_NAME}"
        )
        refreshStatus()
    }

    private fun refreshStatus() {
        lifecycleScope.launch {
            WorkDebugRecorder.record(
                this@MainActivity,
                "PERIODIC_WORK_QUERY",
                "START",
                "uniqueName=${PeriodicLogWorker.UNIQUE_WORK_NAME}"
            )
            val snapshotBundle = withContext(Dispatchers.IO) {
                val workManager = WorkManager.getInstance(this@MainActivity)
                val workInfos = workManager
                    .getWorkInfosForUniqueWork(PeriodicLogWorker.UNIQUE_WORK_NAME)
                    .get()
                SnapshotBundle(
                    querySnapshot = buildQuerySnapshot(workInfos),
                    jobSchedulerSnapshot = buildJobSchedulerSnapshot(),
                )
            }
            WorkDebugRecorder.record(
                this@MainActivity,
                "PERIODIC_WORK_QUERY",
                "RESULT",
                "workId=${snapshotBundle.querySnapshot.workId}, state=${snapshotBundle.querySnapshot.state}, workCount=${snapshotBundle.querySnapshot.workCount}, lastExecuted=${snapshotBundle.querySnapshot.lastExecutedAt}"
            )
            snapshotBundle.querySnapshot.nextSuggestedCheckAt?.let { suggestedCheckAt ->
                WorkDebugRecorder.record(
                    this@MainActivity,
                    "PERIODIC_WORK_QUERY",
                    "NEXT_CHECK_HINT",
                    "workId=${snapshotBundle.querySnapshot.workId}, suggestedCheckAfter=$suggestedCheckAt, intervalMinutes=${PeriodicLogWorker.INTERVAL_MINUTES}"
                )
            }
            WorkDebugRecorder.record(
                this@MainActivity,
                "JOBSCHEDULER_QUERY",
                "START",
                "service=$packageName/${PeriodicLogWorker.SYSTEM_JOB_SERVICE_CLASS_NAME}"
            )
            WorkDebugRecorder.record(
                this@MainActivity,
                "JOBSCHEDULER_QUERY",
                "RESULT",
                snapshotBundle.jobSchedulerSnapshot.toTraceDetail()
            )
        }
    }

    private fun handleStartupCommand(startupIntent: Intent?) {
        val command = startupIntent?.getStringExtra(WorkDebugRecorder.EXTRA_COMMAND).orEmpty()
        if (command.isEmpty()) return
        startupIntent?.removeExtra(WorkDebugRecorder.EXTRA_COMMAND)
        when (command) {
            WorkDebugRecorder.COMMAND_CREATE_PERIODIC_WORK -> enqueuePeriodicWork()
            WorkDebugRecorder.COMMAND_QUERY_PERIODIC_WORK -> refreshStatus()
            WorkDebugRecorder.COMMAND_CANCEL_PERIODIC_WORK -> cancelPeriodicWork()
            else -> Unit
        }
    }

    private fun updateStageUi(startupIntent: Intent?) {
        when (startupIntent?.getStringExtra(WorkDebugRecorder.EXTRA_COMMAND).orEmpty()) {
            WorkDebugRecorder.COMMAND_CREATE_PERIODIC_WORK -> {
                stageText.setText(R.string.workmanager_stage_foreground)
                stageDescriptionText.setText(R.string.workmanager_stage_foreground_desc)
            }

            WorkDebugRecorder.COMMAND_CANCEL_PERIODIC_WORK -> {
                stageText.setText(R.string.workmanager_stage_cleanup)
                stageDescriptionText.setText(R.string.workmanager_stage_cleanup_desc)
            }

            WorkDebugRecorder.COMMAND_QUERY_PERIODIC_WORK -> {
                stageText.setText(R.string.workmanager_stage_foreground)
                stageDescriptionText.setText(R.string.workmanager_stage_foreground_desc)
            }

            else -> {
                stageText.setText(R.string.workmanager_stage_idle)
                stageDescriptionText.setText(R.string.workmanager_stage_idle_desc)
            }
        }
    }

    private fun buildStartupDetail(startupIntent: Intent?): String {
        val startupCommand = startupIntent?.getStringExtra(WorkDebugRecorder.EXTRA_COMMAND).orEmpty()
        return if (startupCommand.isEmpty()) {
            "startupSource=normal, command=none"
        } else {
            "startupSource=command, command=$startupCommand"
        }
    }

    private fun buildQuerySnapshot(workInfos: List<WorkInfo>): QuerySnapshot {
        val activeWorkInfo = workInfos.activeWorkInfo()
        val workState = activeWorkInfo?.state?.name ?: "NOT_CREATED"
        val lastExecutedAt = PeriodicLogWorker.readLastExecutedAtText(this)
        val lastExecutedAtMillis = PeriodicLogWorker.readLastExecutedAtMillis(this)
        val firstEnqueuedAtMillis = PeriodicLogWorker.readEnqueueRequestedAtMillis(this)
        val workCount = if (activeWorkInfo == null) 0 else 1
        val workId = activeWorkInfo?.id?.toString() ?: "none"
        val nextSuggestedCheckAt = if (activeWorkInfo == null || activeWorkInfo.state.isFinished) {
            null
        } else {
            val baseTime = if (lastExecutedAtMillis != null) {
                lastExecutedAtMillis
            } else {
                firstEnqueuedAtMillis?.plus(TimeUnit.SECONDS.toMillis(PeriodicLogWorker.INITIAL_DELAY_SECONDS))
                    ?: System.currentTimeMillis()
            }
            TRACE_DATE_FORMAT.format(
                Date(
                    if (lastExecutedAtMillis != null) {
                        baseTime + TimeUnit.MINUTES.toMillis(PeriodicLogWorker.INTERVAL_MINUTES + 1)
                    } else {
                        baseTime
                    }
                )
            )
        }
        return QuerySnapshot(
            workId = workId,
            state = workState,
            workCount = workCount,
            lastExecutedAt = lastExecutedAt,
            nextSuggestedCheckAt = nextSuggestedCheckAt,
        )
    }

    private fun buildJobSchedulerSnapshot(): JobSchedulerSnapshot {
        val jobScheduler = getSystemService(JobScheduler::class.java)
        val allPendingJobs = jobScheduler
            ?.allPendingJobs
            .orEmpty()
        val trackedJobs = allPendingJobs
            ?.filter { job ->
                job.service.packageName == packageName &&
                    job.service.className == PeriodicLogWorker.SYSTEM_JOB_SERVICE_CLASS_NAME
            }
            .orEmpty()
        val activeJob = trackedJobs.firstOrNull()
        return JobSchedulerSnapshot(
            jobCount = trackedJobs.size,
            jobId = activeJob?.id?.toString() ?: "none",
            service = activeJob?.service?.flattenToShortString() ?: "none",
            isPeriodic = activeJob?.isPeriodic ?: false,
            isPersisted = activeJob?.isPersisted ?: false,
            requiresCharging = activeJob?.isRequireCharging ?: false,
            requiresDeviceIdle = activeJob?.isRequireDeviceIdle ?: false,
        )
    }

    private fun List<WorkInfo>.activeWorkInfo(): WorkInfo? {
        return firstOrNull { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
            ?: firstOrNull { it.state == WorkInfo.State.BLOCKED }
            ?: firstOrNull()
    }

    companion object {
        private val TRACE_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    private data class QuerySnapshot(
        val workId: String,
        val state: String,
        val workCount: Int,
        val lastExecutedAt: String,
        val nextSuggestedCheckAt: String?,
    )

    private data class JobSchedulerSnapshot(
        val jobCount: Int,
        val jobId: String,
        val service: String,
        val isPeriodic: Boolean,
        val isPersisted: Boolean,
        val requiresCharging: Boolean,
        val requiresDeviceIdle: Boolean,
    ) {
        fun toTraceDetail(): String {
            return "jobCount=$jobCount, jobId=$jobId, service=$service, isPeriodic=$isPeriodic, isPersisted=$isPersisted, requiresCharging=$requiresCharging, requiresDeviceIdle=$requiresDeviceIdle"
        }
    }

    private data class SnapshotBundle(
        val querySnapshot: QuerySnapshot,
        val jobSchedulerSnapshot: JobSchedulerSnapshot,
    )
}
