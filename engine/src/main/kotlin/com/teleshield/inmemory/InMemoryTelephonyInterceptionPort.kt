package com.teleshield.inmemory

import com.teleshield.ports.TelephonyInterceptionPort

class InMemoryTelephonyInterceptionPort : TelephonyInterceptionPort {

    var rejectedCount = 0
        private set
    var notificationSuppressedCount = 0
        private set
    var callLogSuppressedCount = 0
        private set

    override fun reject() {
        rejectedCount++
    }

    override fun suppressNotification() {
        notificationSuppressedCount++
    }

    override fun suppressCallLog() {
        callLogSuppressedCount++
    }
}
