package com.kalicyh.onemate

import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions
import android.health.connect.InsertRecordsResponse
import android.health.connect.datatypes.BasalMetabolicRateRecord
import android.health.connect.datatypes.BodyFatRecord
import android.health.connect.datatypes.BodyWaterMassRecord
import android.health.connect.datatypes.BoneMassRecord
import android.health.connect.datatypes.HeightRecord
import android.health.connect.datatypes.LeanBodyMassRecord
import android.health.connect.datatypes.Metadata
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.WeightRecord
import android.health.connect.datatypes.units.Length
import android.health.connect.datatypes.units.Mass
import android.health.connect.datatypes.units.Percentage
import android.health.connect.datatypes.units.Power
import android.os.OutcomeReceiver
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.concurrent.Executors
import java.util.regex.Pattern

object HealthConnectSync {
    const val PREFS = "health_sync"
    const val KEY_ENABLED = "enabled"
    const val KEY_LAST_STATUS = "last_status"
    const val KEY_LAST_RECORD_TIME = "last_record_time"
    const val KEY_LAST_CAPTURE_TIME = "last_capture_time"
    const val LATEST_FILE = "aijk_body_latest.json"

    val permissions = arrayOf(
        HealthPermissions.WRITE_BASAL_METABOLIC_RATE,
        HealthPermissions.WRITE_BODY_FAT,
        HealthPermissions.WRITE_BODY_WATER_MASS,
        HealthPermissions.WRITE_BONE_MASS,
        HealthPermissions.WRITE_HEIGHT,
        HealthPermissions.WRITE_LEAN_BODY_MASS,
        HealthPermissions.WRITE_WEIGHT,
    )

    fun saveAndMaybeSync(context: Context, response: String, done: () -> Unit = {}) {
        Thread {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            runCatching {
                File(context.filesDir, LATEST_FILE).writeText(response)
                val now = System.currentTimeMillis()
                prefs.edit()
                    .putLong(KEY_LAST_CAPTURE_TIME, now)
                    .putString(KEY_LAST_STATUS, "已捕获 AQ 身材数据")
                    .apply()
                if (prefs.getBoolean(KEY_ENABLED, false)) {
                    insert(context, response, done)
                } else {
                    done()
                }
            }.onFailure {
                prefs.edit()
                    .putString(KEY_LAST_STATUS, "保存失败：${it.message.orEmpty()}")
                    .apply()
                done()
            }
        }.start()
    }

    fun hasPermissions(context: Context): Boolean =
        permissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun insert(context: Context, response: String, done: () -> Unit) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!hasPermissions(context)) {
            prefs.edit().putString(KEY_LAST_STATUS, "等待 Health Connect 授权").apply()
            done()
            return
        }
        val manager = context.getSystemService(HealthConnectManager::class.java)
        if (manager == null) {
            prefs.edit().putString(KEY_LAST_STATUS, "Health Connect 不可用").apply()
            done()
            return
        }

        val records = runCatching { buildRecords(response) }.getOrElse {
            prefs.edit().putString(KEY_LAST_STATUS, "解析失败：${it.message.orEmpty()}").apply()
            done()
            return
        }
        if (records.isEmpty()) {
            prefs.edit().putString(KEY_LAST_STATUS, "没有可写入 Health Connect 的字段").apply()
            done()
            return
        }

        val executor = Executors.newSingleThreadExecutor()
        manager.insertRecords(records, executor, object : OutcomeReceiver<InsertRecordsResponse, HealthConnectException> {
            override fun onResult(result: InsertRecordsResponse) {
                prefs.edit()
                    .putString(KEY_LAST_STATUS, "已写入 Health Connect：${records.size} 项")
                    .putLong(KEY_LAST_RECORD_TIME, latestRecordTime(response))
                    .apply()
                executor.shutdown()
                done()
            }

            override fun onError(error: HealthConnectException) {
                prefs.edit()
                    .putString(KEY_LAST_STATUS, "写入失败：${error.message.orEmpty()}")
                    .apply()
                executor.shutdown()
                done()
            }
        })
    }

    fun buildRecords(response: String): List<Record> {
        val root = JSONObject(response)
        val data = root.getJSONObject("data")
        val day = data.getJSONObject("day")
        val record = latestRecord(day)
        val timeMs = record.getLong("recordTime")
        val time = Instant.ofEpochMilli(timeMs)
        val records = mutableListOf<Record>()

        val weightKg = record.optDoubleValue("weight") ?: return emptyList()
        val fatMassKg = record.metricValue("fatMass")
        records += WeightRecord.Builder(meta("weight", timeMs), time, Mass.fromGrams(weightKg * 1000.0)).build()
        record.metricValueOrNull("fatPercent")?.let {
            records += BodyFatRecord.Builder(meta("body-fat", timeMs), time, Percentage.fromValue(it)).build()
        }
        record.metricValueOrNull("bodyWaterMass")?.let {
            records += BodyWaterMassRecord.Builder(meta("body-water", timeMs), time, Mass.fromGrams(it * 1000.0)).build()
        }
        record.metricValueOrNull("boneMass")?.let {
            records += BoneMassRecord.Builder(meta("bone-mass", timeMs), time, Mass.fromGrams(it * 1000.0)).build()
        }
        if (fatMassKg != null) {
            records += LeanBodyMassRecord.Builder(
                meta("lean-body-mass", timeMs),
                time,
                Mass.fromGrams((weightKg - fatMassKg).coerceAtLeast(0.0) * 1000.0),
            ).build()
        }
        day.optJSONObject("todayData")?.optDoubleValue("height")?.let {
            records += HeightRecord.Builder(meta("height", timeMs), time, Length.fromMeters(it / 100.0)).build()
        }
        basalKcalPerDay(data.optString("insightText"))?.let {
            records += BasalMetabolicRateRecord.Builder(
                meta("bmr", timeMs),
                time,
                Power.fromWatts(it * 4184.0 / 86400.0),
            ).build()
        }
        return records
    }

    private fun latestRecord(day: JSONObject): JSONObject {
        val records = day.getJSONArray("records")
        var latest = records.getJSONObject(0)
        for (i in 1 until records.length()) {
            val candidate = records.getJSONObject(i)
            if (candidate.optLong("recordTime") > latest.optLong("recordTime")) {
                latest = candidate
            }
        }
        return latest
    }

    private fun latestRecordTime(response: String): Long =
        latestRecord(JSONObject(response).getJSONObject("data").getJSONObject("day")).optLong("recordTime")

    private fun meta(type: String, timeMs: Long): Metadata =
        Metadata.Builder()
            .setClientRecordId("aijk-a1-$type-$timeMs")
            .setClientRecordVersion(timeMs)
            .setRecordingMethod(Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED)
            .build()

    private fun JSONObject.metricValueOrNull(key: String): Double? =
        optJSONObject(key)?.optDoubleValue("value")

    private fun JSONObject.metricValue(key: String): Double? =
        metricValueOrNull(key)

    private fun JSONObject.optDoubleValue(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key)
        return value.toDoubleOrNull()
    }

    private fun basalKcalPerDay(text: String): Double? {
        val matcher = Pattern.compile("基础代谢为\\s*([0-9.]+)\\s*kcal").matcher(text)
        return if (matcher.find()) matcher.group(1)?.toDoubleOrNull() else null
    }
}
