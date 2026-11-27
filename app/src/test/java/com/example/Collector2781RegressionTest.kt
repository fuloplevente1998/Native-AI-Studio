package com.example

import com.repforge.app.BuildConfig
import com.repforge.app.PrivacyPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Collector2781RegressionTest {
    @Test fun inAppPrivacyPolicyHasTheRealContactUrlAndTheCurrentAppVersion() {
        for (lang in listOf("hu", "en", "de", "ro")) {
            val policy = PrivacyPolicy.text(lang)
            assertFalse("dead GitHub contact in $lang", policy.replace("https://github.com/Kyant0/AndroidLiquidGlass", "").contains("github.com"))
            assertFalse("stale version in $lang", policy.contains("1.2.6"))
            assertFalse("pre-release placeholder in $lang", policy.contains("Play") && policy.contains("rögzítendő"))
            assertFalse("legacy WebView wording in $lang", policy.contains("JavaScript"))
            assertTrue("version in $lang", policy.contains(BuildConfig.VERSION_NAME) && policy.contains(BuildConfig.VERSION_CODE.toString()))
            assertTrue("contact in $lang", policy.contains(BuildConfig.SUPPORT_EMAIL))
            assertTrue("policy URL in $lang", policy.contains(BuildConfig.PRIVACY_POLICY_URL))
        }
        assertTrue(BuildConfig.PRIVACY_POLICY_URL.startsWith("https://fuloplevente1998.github.io/TrainPilot.io/"))
    }

    @Test fun thirdPartyLicensesListEveryNoticedLibraryInAllLanguages() {
        val headings = mapOf("hu" to "Harmadik fél licencek", "en" to "Third-party licenses",
            "de" to "Drittanbieterlizenzen", "ro" to "Licențe ale terților")
        for ((lang, heading) in headings) {
            val section = PrivacyPolicy.text(lang).substringAfter(heading, "")
            for (required in listOf("io.github.kyant0:backdrop:2.0.1", "io.github.kyant0:shapes:1.2.1",
                "© Kyant (Kyant0)", "Apache License 2.0", "https://github.com/Kyant0/AndroidLiquidGlass",
                "third-party-licenses/kyant-backdrop-NOTICE.txt")) {
                assertTrue("$lang missing $required", section.contains(required))
            }
        }
    }

    @Test fun aboutRowShowsTheInstalledBuildAndOpensTheWebsite() {
        val line = com.example.ui.screens.settingsAboutLine(com.example.model.AppLanguage.HU)
        assertTrue(line, line.startsWith("Verzió: TrainPilot ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"))
        assertTrue(line.contains(BuildConfig.DEVELOPER_NAME) && line.contains(BuildConfig.SUPPORT_EMAIL))
        org.junit.Assert.assertEquals("https://fuloplevente1998.github.io/TrainPilot.io/", com.example.ui.screens.settingsWebsiteUrl())
    }
}
