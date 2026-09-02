package com.teleshield.app.screening

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface ScreeningRoleController {
    fun isRoleHeld(): Boolean
    fun requestRoleIntent(): Intent
}

class AndroidScreeningRoleController @Inject constructor(
    @ApplicationContext private val context: Context,
) : ScreeningRoleController {

    private val roleManager: RoleManager?
        get() = context.getSystemService(RoleManager::class.java)

    override fun isRoleHeld(): Boolean =
        roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) ?: false

    override fun requestRoleIntent(): Intent =
        roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            ?: createFallbackIntent()

    private fun createFallbackIntent(): Intent {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }
}
