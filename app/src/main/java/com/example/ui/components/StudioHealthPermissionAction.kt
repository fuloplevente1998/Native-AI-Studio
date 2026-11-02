package com.example.ui.components

/** Read the actual grant before and after consent; a successful dialog alone is not permission. */
internal suspend fun runStudioHealthPermissionAction(
    hasReadPermission: suspend () -> Boolean,
    requestPermission: suspend () -> Unit,
    guard: () -> Unit,
    sync: suspend () -> Unit
) {
    guard()
    if (!hasReadPermission()) {
        requestPermission()
        guard()
        check(hasReadPermission()) { "Nincs olvasási engedély; a szinkron nem indult el." }
    }
    guard()
    sync()
}
