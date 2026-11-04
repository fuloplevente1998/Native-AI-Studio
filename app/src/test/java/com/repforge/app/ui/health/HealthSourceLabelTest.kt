package com.repforge.app.ui.health

import org.junit.Assert.assertEquals
import org.junit.Test

/** The source identifier must remain internal, even when its app label is absent. */
class HealthSourceLabelTest {
    @Test fun suppliedAppLabelIsUsedInFilterAndRecordCards() {
        assertEquals("My Fitness App", friendlyHealthSourceName(
            "com.example.fitness.tracker", "My Fitness App", "Health Connect"
        ))
        assertEquals("Fitbit 2.0", friendlyHealthSourceName(
            "com.fitbit.FitbitMobile", "Fitbit 2.0", "Health Connect"
        ))
    }

    @Test fun wellKnownPackageIdsDisplayFriendlyAppNames() {
        assertEquals("Samsung Health", friendlyHealthSourceName(
            "com.sec.android.app.shealth", "", "Health Connect"
        ))
        assertEquals("Google Fit", friendlyHealthSourceName(
            "com.google.android.apps.fitness", "", "Health Connect"
        ))
        assertEquals("Health Connect", friendlyHealthSourceName(
            "com.google.android.apps.healthdata", "", "Health Connect"
        ))
    }

    @Test fun rawPackageIdIsNotDisplayedWhenSourceLabelIsUnavailable() {
        assertEquals("Health Connect", friendlyHealthSourceName(
            "com.example.fitness.tracker", "com.example.fitness.tracker", "Health Connect"
        ))
        assertEquals("Samsung Health", friendlyHealthSourceName(
            "com.sec.android.app.shealth", "com.sec.android.app.shealth", "Health Connect"
        ))
    }
}
