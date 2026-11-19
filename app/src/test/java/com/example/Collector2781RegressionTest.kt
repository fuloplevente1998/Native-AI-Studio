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
            assertFalse("dead GitHub contact in $lang", policy.contains("github.com"))
            assertFalse("stale version in $lang", policy.contains("1.2.6"))
            assertFalse("pre-release placeholder in $lang", policy.contains("Play") && policy.contains("rögzítendő"))
            assertFalse("legacy WebView wording in $lang", policy.contains("JavaScript"))
            assertTrue("version in $lang", policy.contains(BuildConfig.VERSION_NAME) && policy.contains(BuildConfig.VERSION_CODE.toString()))
            assertTrue("contact in $lang", policy.contains(BuildConfig.SUPPORT_EMAIL))
            assertTrue("policy URL in $lang", policy.contains(BuildConfig.PRIVACY_POLICY_URL))
        }
        assertTrue(BuildConfig.PRIVACY_POLICY_URL.startsWith("https://fuloplevente1998.github.io/TrainPilot.io/"))
    }
}
