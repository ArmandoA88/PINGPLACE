package com.pingplace.model

enum class TriggerType {
    DISTANCE,
    TRAVEL_TIME
}

enum class ReminderPriority {
    LOW,
    NORMAL,
    HIGH
}

enum class ReminderRepeatType {
    NONE,
    DAILY,
    WEEKDAYS,
    WEEKLY
}

enum class BlockedTimeBehavior {
    RESPECT_GLOBAL,
    IGNORE_GLOBAL,
    CUSTOM_ALLOWED_HOURS
}

enum class UnitsSystem {
    IMPERIAL,
    METRIC
}

enum class ReminderFilter {
    ACTIVE,
    COMPLETED,
    SNOOZED,
    SUPPRESSED
}

data class BrandOption(
    val name: String,
    val query: String,
    val isCategory: Boolean = false
)

object BrandCatalog {
    val defaults = listOf(
        BrandOption("Whole Foods", "Whole Foods"),
        BrandOption("UPS Store", "UPS Store"),
        BrandOption("Costco", "Costco"),
        BrandOption("Walmart", "Walmart"),
        BrandOption("Target", "Target"),
        BrandOption("CVS", "CVS"),
        BrandOption("Walgreens", "Walgreens"),
        BrandOption("Post Office", "Post Office", isCategory = true),
        BrandOption("Home Depot", "Home Depot"),
        BrandOption("Pharmacy", "Pharmacy", isCategory = true)
    )
}
