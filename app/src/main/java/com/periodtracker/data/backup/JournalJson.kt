package com.periodtracker.data.backup

import com.periodtracker.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

object JournalJson {
    fun encodePreferences(p: UserPreferences): String = JSONObject().apply {
        put("onboardingComplete", p.onboardingComplete); put("onboardingStep", p.onboardingStep)
        put("draftStart", p.draftStart); put("draftEnd", p.draftEnd)
        put("cycleLength", p.cycle.usualCycleLength); put("periodDuration", p.cycle.usualPeriodDuration)
        put("appearance", p.appearance.name); put("upcoming", p.upcomingEnabled); put("daily", p.dailyEnabled)
        put("leadDays", p.leadDays); put("time", p.reminderTime.toString()); put("discreet", p.discreet)
    }.toString()

    fun decodePreferences(json: String): UserPreferences = JSONObject(json).let { p ->
        UserPreferences(p.getBoolean("onboardingComplete"), p.getInt("onboardingStep"),
            p.getString("draftStart"), p.getString("draftEnd"),
            CyclePreferences(p.getInt("cycleLength"), p.getInt("periodDuration")),
            Appearance.valueOf(p.getString("appearance")), p.getBoolean("upcoming"), p.getBoolean("daily"),
            p.getInt("leadDays"), LocalTime.parse(p.getString("time")), p.getBoolean("discreet"))
            .also { it.validate() }
    }

    fun encode(snapshot: JournalSnapshot): ByteArray = JSONObject().apply {
        put("schemaVersion", 1)
        put("preferences", JSONObject(encodePreferences(snapshot.preferences)))
        put("periods", JSONArray().apply { snapshot.periods.forEach { p -> put(JSONObject().apply {
            put("id", p.id); put("start", p.start.toString()); put("end", p.end?.toString() ?: JSONObject.NULL)
            put("status", p.status.name); put("createdAt", p.createdAt); put("updatedAt", p.updatedAt)
        }) } })
        put("logs", JSONArray().apply { snapshot.logs.forEach { l -> put(JSONObject().apply {
            put("date", l.date.toString()); put("flow", l.flow?.name ?: JSONObject.NULL)
            put("mood", l.mood?.name ?: JSONObject.NULL); put("pain", l.pain?.name ?: JSONObject.NULL)
            put("notes", l.notes); put("noSymptoms", l.noSymptomsExplicit ?: JSONObject.NULL)
            put("symptoms", JSONArray(l.symptoms.map { it.name }))
            put("createdAt", l.createdAt); put("updatedAt", l.updatedAt)
        }) } })
    }.toString().toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray, today: LocalDate): JournalSnapshot {
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        require(root.getInt("schemaVersion") == 1) { "This backup version is not supported." }
        val periods = root.getJSONArray("periods").objects().map { p ->
            Period(p.getString("id"), LocalDate.parse(p.getString("start")),
                p.nullableString("end")?.let(LocalDate::parse), PeriodStatus.valueOf(p.getString("status")),
                p.getLong("createdAt"), p.getLong("updatedAt"))
        }
        val logs = root.getJSONArray("logs").objects().map { l ->
            val symptoms = l.getJSONArray("symptoms").let { a -> (0 until a.length()).map { Symptom.valueOf(a.getString(it)) } }
            require(symptoms.distinct().size == symptoms.size) { "Duplicate symptoms in backup." }
            DailyLog(LocalDate.parse(l.getString("date")), l.nullableString("flow")?.let(FlowLevel::valueOf),
                l.nullableString("mood")?.let(Mood::valueOf), l.nullableString("pain")?.let(Pain::valueOf),
                l.getString("notes"), symptoms.toSet(), if (l.isNull("noSymptoms")) null else l.getBoolean("noSymptoms"),
                l.getLong("createdAt"), l.getLong("updatedAt"))
        }
        return JournalSnapshot(periods, logs, decodePreferences(root.getJSONObject("preferences").toString()))
            .also { validate(it, today) }
    }

    fun validate(snapshot: JournalSnapshot, today: LocalDate) {
        snapshot.preferences.validate()
        PeriodValidation.validate(snapshot.periods, today)
        require(snapshot.logs.map { it.date }.distinct().size == snapshot.logs.size) { "Duplicate daily records in backup." }
        snapshot.logs.forEach { it.validate(today); require(!it.isEmpty) { "Empty daily record in backup." } }
        require(snapshot.periods.all { it.createdAt >= 0 && it.updatedAt >= it.createdAt } &&
            snapshot.logs.all { it.createdAt >= 0 && it.updatedAt >= it.createdAt }) { "Invalid record timestamps." }
    }
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
}
