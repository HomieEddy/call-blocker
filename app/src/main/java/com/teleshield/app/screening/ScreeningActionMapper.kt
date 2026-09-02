package com.teleshield.app.screening

import com.teleshield.domain.ScreeningVerdict

object ScreeningActionMapper {

    fun toAction(verdict: ScreeningVerdict): ScreeningAction = when (verdict) {
        is ScreeningVerdict.Blocked -> ScreeningAction.REJECT
        is ScreeningVerdict.Whitelisted -> ScreeningAction.ALLOW
        is ScreeningVerdict.Allowed -> ScreeningAction.ALLOW
    }
}
