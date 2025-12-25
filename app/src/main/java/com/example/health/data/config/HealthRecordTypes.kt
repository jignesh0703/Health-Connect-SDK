package com.example.health.data.config

import androidx.health.connect.client.records.*
import kotlin.reflect.KClass

/**
 * Configuration class defining all Health Connect record types organized by category.
 * This class provides a simple way to fetch all health data types using loops.
 */
object HealthRecordTypes {
    /**
     * Data class representing a health record type configuration.
     * 
     * @property recordClass The record class type
     * @property category The category name (e.g., "Activity", "Vitals")
     * @property displayName The human-readable display name
     */
    data class RecordTypeConfig(
        val recordClass: KClass<out Record>,
        val category: String,
        val displayName: String
    )

    /**
     * All Activity-related record types.
     */
    val ACTIVITY_RECORDS = listOf(
        RecordTypeConfig(ActiveCaloriesBurnedRecord::class, "Activity", "Active Calories Burned"),
        RecordTypeConfig(ActivityIntensityRecord::class, "Activity", "Activity Intensity"),
        RecordTypeConfig(CyclingPedalingCadenceRecord::class, "Activity", "Cycling Pedaling Cadence"),
        RecordTypeConfig(DistanceRecord::class, "Activity", "Distance"),
        RecordTypeConfig(ElevationGainedRecord::class, "Activity", "Elevation Gained"),
        RecordTypeConfig(ExerciseSessionRecord::class, "Activity", "Exercise"),
        RecordTypeConfig(FloorsClimbedRecord::class, "Activity", "Floors Climbed"),
        RecordTypeConfig(PlannedExerciseSessionRecord::class, "Activity", "Planned Exercise"),
        RecordTypeConfig(PowerRecord::class, "Activity", "Power"),
        RecordTypeConfig(SexualActivityRecord::class, "Activity", "Sexual Activity"),
        RecordTypeConfig(SpeedRecord::class, "Activity", "Speed"),
        RecordTypeConfig(StepsRecord::class, "Activity", "Steps"),
        RecordTypeConfig(TotalCaloriesBurnedRecord::class, "Activity", "Total Calories Burned"),
        RecordTypeConfig(Vo2MaxRecord::class, "Activity", "VO2 Max"),
        RecordTypeConfig(WheelchairPushesRecord::class, "Activity", "Wheelchair Pushes")
    )

    /**
     * All Body Measurement-related record types.
     */
    val BODY_MEASUREMENT_RECORDS = listOf(
        RecordTypeConfig(BasalMetabolicRateRecord::class, "Body Measurement", "Basal Metabolic Rate"),
        RecordTypeConfig(BodyFatRecord::class, "Body Measurement", "Body Fat"),
        RecordTypeConfig(BodyWaterMassRecord::class, "Body Measurement", "Body Water Mass"),
        RecordTypeConfig(BoneMassRecord::class, "Body Measurement", "Bone Mass"),
        RecordTypeConfig(HeightRecord::class, "Body Measurement", "Height"),
        RecordTypeConfig(WeightRecord::class, "Body Measurement", "Weight"),
        RecordTypeConfig(LeanBodyMassRecord::class, "Body Measurement", "Lean Body Mass")
    )

    /**
     * All Cycle Tracking-related record types.
     */
    val CYCLE_TRACKING_RECORDS = listOf(
        RecordTypeConfig(BasalBodyTemperatureRecord::class, "Cycle Tracking", "Basal Body Temperature"),
        RecordTypeConfig(CervicalMucusRecord::class, "Cycle Tracking", "Cervical Mucus"),
        RecordTypeConfig(IntermenstrualBleedingRecord::class, "Cycle Tracking", "Intermenstrual Bleeding"),
        RecordTypeConfig(MenstruationFlowRecord::class, "Cycle Tracking", "Menstruation"),
        RecordTypeConfig(OvulationTestRecord::class, "Cycle Tracking", "Ovulation Test")
    )

    /**
     * All Nutrition-related record types.
     */
    val NUTRITION_RECORDS = listOf(
        RecordTypeConfig(HydrationRecord::class, "Nutrition", "Hydration"),
        RecordTypeConfig(NutritionRecord::class, "Nutrition", "Nutrition")
    )

    /**
     * All Sleep-related record types.
     */
    val SLEEP_RECORDS = listOf(
        RecordTypeConfig(SleepSessionRecord::class, "Sleep", "Sleep Session")
    )

    /**
     * All Vitals-related record types.
     */
    val VITALS_RECORDS = listOf(
        RecordTypeConfig(BloodGlucoseRecord::class, "Vitals", "Blood Glucose"),
        RecordTypeConfig(BloodPressureRecord::class, "Vitals", "Blood Pressure"),
        RecordTypeConfig(BodyTemperatureRecord::class, "Vitals", "Body Temperature"),
        RecordTypeConfig(HeartRateRecord::class, "Vitals", "Heart Rate"),
        RecordTypeConfig(HeartRateVariabilityRmssdRecord::class, "Vitals", "Heart Rate Variability"),
        RecordTypeConfig(OxygenSaturationRecord::class, "Vitals", "Oxygen Saturation"),
        RecordTypeConfig(RespiratoryRateRecord::class, "Vitals", "Respiratory Rate"),
        RecordTypeConfig(RestingHeartRateRecord::class, "Vitals", "Resting Heart Rate"),
        RecordTypeConfig(SkinTemperatureRecord::class, "Vitals", "Skin Temperature")
    )

    /**
     * All Wellness-related record types.
     */
    val WELLNESS_RECORDS = listOf<RecordTypeConfig>(
        RecordTypeConfig(MindfulnessSessionRecord::class, "Wellness", "Mindfulness")
    )

    /**
     * All record types combined, organized by category.
     */
    val ALL_RECORDS_BY_CATEGORY = mapOf(
        "Activity" to ACTIVITY_RECORDS,
        "Body Measurement" to BODY_MEASUREMENT_RECORDS,
        "Cycle Tracking" to CYCLE_TRACKING_RECORDS,
        "Nutrition" to NUTRITION_RECORDS,
        "Sleep" to SLEEP_RECORDS,
        "Vitals" to VITALS_RECORDS,
        "Wellness" to WELLNESS_RECORDS
    )

    /**
     * Flat list of all record types.
     */
    val ALL_RECORDS = ALL_RECORDS_BY_CATEGORY.values.flatten()

    /**
     * Gets all record types for a specific category.
     * 
     * @param category The category name
     * @return List of record type configurations for the category
     */
    fun getRecordsByCategory(category: String): List<RecordTypeConfig> {
        return ALL_RECORDS_BY_CATEGORY[category] ?: emptyList()
    }
}
