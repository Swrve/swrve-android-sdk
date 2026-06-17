package com.swrve.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Validates FreeMarker expression compatibility using a real template and generated output from
 * Accelerator's Email channel. The template was extracted from a working Email campaign and the
 * expected output files represent what Accelerator produced for two property profiles.
 *
 * These tests complement the conformance suite (SwrveFreemarkerConformanceTest) which is more
 * comprehensive and was generated against the reference FreeMarker Java library.
 */
class SwrveFreemarkerEmailCompatibilityTest : SwrveFreemarkerTestBase() {

    // All recipient properties present, including uppercase boolean "TRUE" as Email surfaces it.
    private val allProps = mapOf(
        "Recipient.FIRST_NAME" to "  John  ",
        "Recipient.LOYALTY_POINTS" to "1200",
        "Recipient.COUNTRY" to "US",
        "Recipient.PROMO_CODE" to "SAVE20",
        "Recipient.IS_PREMIUM" to "TRUE"
        // Recipient.NONEXISTENT_PROP intentionally absent
    )

    // Partial properties to exercise fallback and missing-property handling.
    private val missingProps = mapOf(
        "Recipient.FIRST_NAME" to " Jane ",
        "Recipient.LOYALTY_POINTS" to "850",
        "Recipient.COUNTRY" to "UK"
        // Recipient.PROMO_CODE, IS_PREMIUM, NONEXISTENT_PROP intentionally absent
    )

    @Test
    fun testEmailTemplateWithAllProperties() {
        val template = readResource("freemarker_email_template.txt")
        val expected = readResource("freemarker_email_expected_all_props.txt")
        assertEquals(expected, eval(template, allProps))
    }

    @Test
    fun testEmailTemplateWithMissingProperties() {
        val template = readResource("freemarker_email_template.txt")
        val expected = readResource("freemarker_email_expected_missing_props.txt")
        assertEquals(expected, eval(template, missingProps))
    }

    private fun readResource(filename: String): String =
        requireNotNull(javaClass.classLoader!!.getResourceAsStream(filename)) {
            "$filename not found in test resources"
        }.use { it.bufferedReader(Charsets.UTF_8).readText() }
}
