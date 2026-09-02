package com.teleshield.domain

data class ScreeningConfiguration(
    val masterScreeningEnabled: Boolean,
    val blockUnknownEnabled: Boolean,
    val logRetentionDays: Int,
)
