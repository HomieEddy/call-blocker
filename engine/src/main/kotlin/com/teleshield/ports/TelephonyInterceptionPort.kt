package com.teleshield.ports

interface TelephonyInterceptionPort {
    fun reject()
    fun suppressNotification()
    fun suppressCallLog()
}
