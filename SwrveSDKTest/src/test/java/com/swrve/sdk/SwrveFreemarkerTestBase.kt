package com.swrve.sdk

import org.junit.Assert.assertTrue
import org.junit.Assert.fail

abstract class SwrveFreemarkerTestBase {

    protected fun eval(template: String, props: Map<String, Any> = emptyMap(), local: Boolean = false): String =
        SwrveFreemarkerEvaluator.evaluate(template, props, local)

    protected fun assertSuppresses(block: () -> Unit) {
        try {
            block()
            fail("Expected FreemarkerException was not thrown")
        } catch (e: FreemarkerException) {
            // expected
        }
    }

    protected fun assertSuppressesWithMessage(expectedFragment: String, block: () -> Unit) {
        try {
            block()
            fail("Expected FreemarkerException was not thrown")
        } catch (e: FreemarkerException) {
            assertTrue(
                "Expected message to contain '$expectedFragment' but was: ${e.message}",
                e.message?.contains(expectedFragment) == true
            )
        }
    }
}
