package com.burton.sonos.report

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.widget.Toast
import kotlin.math.sqrt

object BurtonIssues {
    const val ACTION_CREATE = "com.burton.issues.action.CREATE_ISSUE"
    const val RELEASE_PACKAGE = "com.burton.issues"
    const val DEBUG_PACKAGE = "com.burton.issues.debug"

    fun catalogPackage(packageName: String): String = packageName.removeSuffix(".debug")

    fun newIssueUri(packageName: String): String =
        "burtonissues://new?package=${catalogPackage(packageName)}"

    fun openNewIssue(context: Context) {
        val id = catalogPackage(context.packageName)
        val flags = if (context is Activity) 0 else Intent.FLAG_ACTIVITY_NEW_TASK
        val extras = Intent(ACTION_CREATE)
            .putExtra("package", id)
            .addFlags(flags)
        for (pkg in listOf(RELEASE_PACKAGE, DEBUG_PACKAGE)) {
            if (tryStart(context, Intent(extras).setPackage(pkg))) return
        }
        if (tryStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(newIssueUri(id))).addFlags(flags))) {
            return
        }
        Toast.makeText(context, "Install Burton Issues to file a bug.", Toast.LENGTH_SHORT).show()
    }

    private fun tryStart(context: Context, intent: Intent): Boolean {
        if (intent.resolveActivity(context.packageManager) == null) return false
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

class ShakeDetector(
    private val nowMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val onShake: () -> Unit,
) {
    companion object {
        const val G_THRESHOLD = 2.7f
        const val WINDOW_MS = 1_500L
        const val COOLDOWN_MS = 2_000L
        const val PEAKS_REQUIRED = 2
        const val GRAVITY = 9.80665f
    }

    private val peaks = ArrayDeque<Long>()
    private var lastFiredAt = Long.MIN_VALUE / 2

    fun onAcceleration(x: Float, y: Float, z: Float): Boolean {
        val gForce = sqrt(x * x + y * y + z * z) / GRAVITY
        if (gForce < G_THRESHOLD) return false
        val now = nowMs()
        if (now - lastFiredAt < COOLDOWN_MS) return false
        while (peaks.isNotEmpty() && now - peaks.first() > WINDOW_MS) {
            peaks.removeFirst()
        }
        peaks.addLast(now)
        if (peaks.size < PEAKS_REQUIRED) return false
        peaks.clear()
        lastFiredAt = now
        onShake()
        return true
    }
}

class ShakeToReport(
    private val activity: Activity,
    private val onShake: () -> Unit = { BurtonIssues.openNewIssue(activity) },
) : SensorEventListener {
    private val detector = ShakeDetector(onShake = {
        activity.window?.decorView?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        onShake()
    })
    private var manager: SensorManager? = null

    fun start() {
        val sm = activity.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        manager = sm
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        manager?.unregisterListener(this)
        manager = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        detector.onAcceleration(event.values[0], event.values[1], event.values[2])
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
