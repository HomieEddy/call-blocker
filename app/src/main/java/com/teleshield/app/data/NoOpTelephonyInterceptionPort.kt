package com.teleshield.app.data

import com.teleshield.ports.TelephonyInterceptionPort
import javax.inject.Inject

class NoOpTelephonyInterceptionPort @Inject constructor() : TelephonyInterceptionPort {
    override fun reject() = Unit
    override fun suppressNotification() = Unit
    override fun suppressCallLog() = Unit
}
