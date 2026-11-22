package com.example

import com.example.ui.viewmodel.formatProviderLabelHu
import com.repforge.app.HealthJournalStore
import org.junit.Assert.assertEquals
import org.junit.Test

/** #324: no random package suffix or opaque health-source id in user-visible step labels. */
class StudioProviderLabel324Test {
    @Test fun unknownOriginShowsProviderWithoutTechnicalIdentifier() {
        assertEquals("Health Connect",
            formatProviderLabelHu(HealthJournalStore.HC, "jc11f4b421ddb36a29ecd87f514775d"))
        assertEquals("Health Connect",
            formatProviderLabelHu(HealthJournalStore.HC, "org.test.some.device.id"))
        assertEquals("Samsung Health",
            formatProviderLabelHu(HealthJournalStore.SAMSUNG, "unrecognized.provider.id"))
    }

    @Test fun knownExternalOriginKeepsReadableAttribution() {
        assertEquals("Health Connect (Samsung Health)",
            formatProviderLabelHu(HealthJournalStore.HC, "com.sec.android.app.shealth"))
        assertEquals("Health Connect (Google Fit)",
            formatProviderLabelHu(HealthJournalStore.HC, "com.google.android.apps.fitness"))
        assertEquals("Health Connect (Garmin)",
            formatProviderLabelHu(HealthJournalStore.HC, "com.garmin.health"))
    }
}
