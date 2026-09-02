package com.teleshield.app.screening

import android.net.Uri

object CallNumberExtractor {

    fun extract(handle: Uri?): String = when {
        handle == null -> ""
        handle.scheme == "tel" -> handle.schemeSpecificPart ?: ""
        else -> ""
    }
}
