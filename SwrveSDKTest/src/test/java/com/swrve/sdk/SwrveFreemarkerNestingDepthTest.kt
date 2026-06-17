package com.swrve.sdk

import org.junit.Assert.assertEquals
import org.junit.Test

class SwrveFreemarkerNestingDepthTest : SwrveFreemarkerTestBase() {

    @Test
    fun testThreeLevelIfNesting() {
        assertEquals("deep", eval("<#if a == \"1\"><#if b == \"2\"><#if c == \"3\">deep</#if></#if></#if>", mapOf("a" to "1", "b" to "2", "c" to "3")))
    }

    @Test
    fun testFourLevelIfNesting() {
        assertEquals("deep", eval("<#if a == \"1\"><#if b == \"2\"><#if c == \"3\"><#if d == \"4\">deep</#if></#if></#if></#if>", mapOf("a" to "1", "b" to "2", "c" to "3", "d" to "4")))
    }

    @Test
    fun testThreeLevelIfNestingWithElseIf() {
        assertEquals("yes", eval("<#if a == \"1\"><#if b == \"2\"><#if c == \"3\">yes<#elseif c == \"x\">no</#if></#if></#if>", mapOf("a" to "1", "b" to "2", "c" to "3")))
    }

    @Test
    fun testFourLevelIfNestingElseBranch() {
        val template = "<#if a == \"1\"><#if b == \"2\"><#if c == \"3\">ok<#else><#if d == \"4\">deep</#if></#if></#if></#if>"
        // then-branch: c matches, else not entered
        assertEquals("ok", eval(template, mapOf("a" to "1", "b" to "2", "c" to "3", "d" to "4")))
        // else-branch: c doesn't match, 4th-level <#if> inside else is actually executed
        assertEquals("deep", eval(template, mapOf("a" to "1", "b" to "2", "c" to "x", "d" to "4")))
    }

    @Test
    fun testSwitchIfNesting() {
        assertEquals(
            "VIP",
            eval(
                "<#switch Recipient.tier><#case \"gold\"><#if Recipient.balance?number gt 100>VIP<#else>Standard</#if><#break></#switch>",
                mapOf("Recipient.tier" to "gold", "Recipient.balance" to "200")
            )
        )
    }

    @Test
    fun testSwitchIfIfNesting() {
        assertEquals(
            "deep",
            eval("<#switch Recipient.tier><#case \"gold\"><#if a == \"1\"><#if b == \"2\">deep</#if></#if><#break></#switch>", mapOf("Recipient.tier" to "gold", "a" to "1", "b" to "2"))
        )
    }

    @Test
    fun testSwitchIfIfIfNesting() {
        assertEquals(
            "deep",
            eval(
                "<#switch Recipient.tier><#case \"gold\"><#if a == \"1\"><#if b == \"2\"><#if c == \"3\">deep</#if></#if></#if><#break></#switch>",
                mapOf("Recipient.tier" to "gold", "a" to "1", "b" to "2", "c" to "3")
            )
        )
    }

    @Test
    fun testNestingDepthLimitSuppresses() {
        val deep = "<#if a??>".repeat(101) + "x" + "</#if>".repeat(101)
        assertSuppressesWithMessage("depth") { eval(deep, mapOf("a" to "1")) }
    }

    @Test
    fun testNestingDepthAtLimitSucceeds() {
        val deep = "<#if a??>".repeat(100) + "x" + "</#if>".repeat(100)
        assertEquals("x", eval(deep, mapOf("a" to "1")))
    }
}
