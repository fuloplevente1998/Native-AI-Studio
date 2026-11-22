package com.example.ui.viewmodel

import com.repforge.app.HealthJournalStore

/** #324: keep technical dataOrigin IDs in journal/diagnostics, never show opaque packages in UI. */
internal fun formatProviderLabelHu(channel: String, originPkg: String = ""): String {
        val base = when (channel) {
            HealthJournalStore.SAMSUNG -> "Samsung Health"
            HealthJournalStore.HC -> "Health Connect"
            HealthJournalStore.BLE -> "Wear OS / BLE óra"
            HealthJournalStore.LEGACY -> "Helyi egészségnapló"
            else -> "Nincs szinkronizált adat"
        }
        val cleanPkg = originPkg.trim()
        if (cleanPkg.isBlank()) return base
        val shortOrigin = when {
            cleanPkg.contains("shealth", ignoreCase = true) -> "Samsung Health"
            cleanPkg.contains("google.android.apps.fitness", ignoreCase = true) -> "Google Fit"
            cleanPkg.contains("wear", ignoreCase = true) -> "Wear OS"
            cleanPkg.contains("garmin", ignoreCase = true) -> "Garmin"
            else -> return base // Do not leak opaque Health Connect origin package IDs to the UI.
        }
        return if (shortOrigin.equals(base, ignoreCase = true)) base else "$base ($shortOrigin)"
    }

