package com.teleshield.domain

class CallerIdentifier private constructor(
    val raw: String,
    val canonical: String,
    val isAnonymous: Boolean,
) {

    companion object {
        fun from(raw: String, normalizer: IdentifierNormalizer): CallerIdentifier =
            CallerIdentifier(
                raw = raw,
                canonical = normalizer.normalize(raw),
                isAnonymous = normalizer.isAnonymous(raw),
            )
    }
}
