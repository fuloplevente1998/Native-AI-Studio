package com.example.model

/**
 * Location/level shown on a program card (Hungarian source values).
 *
 * Built-in programs always use the bundled catalog metadata: older profile saves wrote the
 * user's own profile location/level (e.g. "Otthon" / "Kezdő") into every built-in row, which
 * made gym programs read "Otthon • Kezdő". Custom and generated plans keep their stored values.
 */
internal fun resolveProgramLocationLevel(
    builtin: Boolean,
    storedLocation: String,
    storedLevel: String,
    templateLocation: String?,
    templateLevel: String?
): Pair<String, String> {
    val catalogLocation = templateLocation?.takeIf { builtin && it.isNotBlank() }
    val catalogLevel = templateLevel?.takeIf { builtin && it.isNotBlank() }
    val location = catalogLocation
        ?: storedLocation.ifBlank { templateLocation.orEmpty() }.ifBlank { "Otthon / Edzőterem" }
    val level = catalogLevel
        ?: storedLevel.ifBlank { templateLevel.orEmpty() }.ifBlank { "Kezdő" }
    return location to level
}
