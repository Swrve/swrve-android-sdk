package com.swrve.sdk

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import freemarker.template.Configuration
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Verifies that `freemarker_conformance_fixtures.json` in the iOS repo is in sync with the
 * current TEST_CASES. Runs as a normal test whenever the ios-sdk repo is checked out alongside
 * this one (standard sdktool layout). Skips silently if the iOS repo is absent.
 *
 * To regenerate the fixture after adding new test cases or bumping the FreeMarker version:
 *   GENERATE_FM_FIXTURES=true ./gradlew :SwrveSDKTest:testCoreDebugUnitTest \
 *     --tests "com.swrve.sdk.SwrveFreemarkerFixtureGenerator"
 */
class SwrveFreemarkerFixtureGenerator {

    private val fmConfig = Configuration(Configuration.VERSION_2_3_23).also {
        it.defaultEncoding = "UTF-8"
    }

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private fun fmEval(templateStr: String, props: Map<String, String>): String =
        SwrveFreemarkerConformanceTest.fmEval(templateStr, props, fmConfig)

    private fun buildFixtureJson(): String {
        val array = SwrveFreemarkerConformanceTest.TEST_CASES.map { c ->
            val fmExpected = fmEval(c.template, c.props)
            assertEquals("FreeMarker output mismatch for ${c.name}", c.expected, fmExpected)
            JsonObject().apply {
                addProperty("name", c.name)
                addProperty("template", c.template)
                add("props", gson.toJsonTree(c.props.toSortedMap()))
                addProperty("expected", fmExpected)
            }
        }
        return gson.toJson(array) + "\n"
    }

    @Test
    fun testFixtureSync() {
        // Gradle runs tests with CWD = android-sdk/public/SwrveSDKTest/.
        val iosFile = File("../../../ios-sdk/public/SwrveSDKTest/SDKTests/Helpers/freemarker_conformance_fixtures.json")

        val generate = System.getenv("GENERATE_FM_FIXTURES") != null
        if (!generate && !iosFile.exists()) return // ios-sdk not checked out — skip silently

        val generated = buildFixtureJson()

        if (generate) {
            check(iosFile.parentFile.isDirectory) {
                "iOS fixture directory not found at ${iosFile.parentFile.absolutePath} — is the ios-sdk repo checked out?"
            }
            iosFile.writeText(generated)
            println("Wrote ${SwrveFreemarkerConformanceTest.TEST_CASES.size} fixtures to ${iosFile.canonicalPath}")
        } else {
            assertEquals(
                "iOS fixture is out of date — run: GENERATE_FM_FIXTURES=true ./gradlew :SwrveSDKTest:testCoreDebugUnitTest --tests \"com.swrve.sdk.SwrveFreemarkerFixtureGenerator\"",
                generated,
                iosFile.readText()
            )
        }
    }
}
