package com.swrve.sdk

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.swrve.sdk.notifications.model.SwrveCarouselItem
import com.swrve.sdk.notifications.model.SwrveNotification
import com.swrve.sdk.notifications.model.SwrveNotificationButton.ActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * carousel_payloads.json is shared verbatim with the iOS SwrveTestCarouselItem fixtures.
 * See docs/features/MG-25558-carousel-payload-contract.md.
 */
class SwrveCarouselItemTest : SwrveBaseTest() {

    private lateinit var fixtures: JsonObject

    @Before
    override fun setUp() {
        super.setUp()
        val json = SwrveTestUtils.getAssetAsText(mActivity!!, "carousel_payloads.json")
        fixtures = JsonParser.parseString(json).asJsonObject
    }

    // On Android _sw arrives as a JSON-encoded string, so each fixture goes through fromJson(String).
    private fun notificationFromFixture(name: String): SwrveNotification {
        val notification = SwrveNotification.fromJson(fixtures.get(name).toString())
        assertNotNull(notification)
        return notification
    }

    private fun carouselFromFixture(name: String): List<SwrveCarouselItem>? = notificationFromFixture(name).carousel

    private fun carouselFromJson(json: String): List<SwrveCarouselItem>? {
        val notification = SwrveNotification.fromJson(json)
        assertNotNull(notification)
        return notification.carousel
    }

    @Test
    fun testCanonicalPayloadParses() {
        val notification = notificationFromFixture("canonical")
        val items = notification.carousel
        assertNotNull(items)
        assertEquals(2, items!!.size)

        assertEquals("$110 off", items[0].title)
        assertEquals("55-inch 4K LED television", items[0].body)
        assertEquals("https://cdn.example.com/carousel/item1.png", items[0].imageUrl)
        assertEquals(ActionType.OPEN_URL, items[0].actionType)
        assertEquals("https://www.example.com/", items[0].action)

        assertEquals("$150 off", items[1].title)
        assertEquals("65-inch OLED television", items[1].body)
        assertEquals("https://cdn.example.com/carousel/item2.png", items[1].imageUrl)
        assertEquals(ActionType.OPEN_APP, items[1].actionType)
        assertEquals("", items[1].action)

        // Coexists with buttons
        assertEquals(1, notification.buttons.size)
        assertEquals("Shop now", notification.buttons[0].title)
    }

    @Test
    fun testEmptyArrayParsesAsEmptyList() {
        val items = carouselFromFixture("empty_array")
        assertNotNull(items)
        assertEquals(0, items!!.size)
    }

    @Test
    fun testMissingCarouselKeyIsNotACarousel() {
        assertNull(carouselFromJson("{\"version\":1}"))
    }

    @Test
    fun testItemMissingImageUrlIsParsedAsSent() {
        // The parser reports the payload as sent. The dashboard guarantees an image_url on every item.
        val items = carouselFromFixture("item_missing_image_url")
        assertNotNull(items)
        assertEquals(2, items!!.size)
        assertEquals("https://cdn.example.com/carousel/item1.png", items[0].imageUrl)
        assertNull(items[1].imageUrl)
        assertEquals("index 1 has no image_url", items[1].title)
    }

    @Test
    fun testItemEmptyImageUrlIsParsedAsSent() {
        val items = carouselFromFixture("item_empty_image_url")
        assertNotNull(items)
        assertEquals(1, items!!.size)
        assertEquals("", items[0].imageUrl)
    }

    @Test
    fun testNotAnArrayFailsToParseTheWholePayload() {
        // Differs from iOS, which ignores just the carousel: the parser trusts the dashboard to send an array (MG-25567)
        assertNull(SwrveNotification.fromJson(fixtures.get("not_an_array").toString()))
    }

    @Test
    fun testEveryFixtureIsRoutedAsCarouselOnlyWhenItHasItems() {
        val expected = mapOf(
            "canonical" to true,
            "empty_array" to false,
            "not_an_array" to false,
            "item_missing_image_url" to true,
            "item_empty_image_url" to true,
            "optional_fields_absent" to true,
            "seven_items_all_retained" to true,
            "action_type_variants" to true,
            "stray_settings_keys_are_ignored" to true
        )
        assertEquals("a fixture was added or removed", fixtures.keySet(), expected.keys)
        expected.forEach { (name, isCarousel) ->
            val data = mapOf(SwrveNotificationConstants.SWRVE_PAYLOAD_KEY to fixtures.get(name).toString())
            assertEquals(name, isCarousel, SwrveHelper.isCarouselPush(data))
        }
    }

    @Test
    fun testOptionalFieldsMayBeAbsent() {
        val items = carouselFromFixture("optional_fields_absent")
        assertNotNull(items)
        assertEquals(4, items!!.size)

        // Index 1 carries only image_url
        assertEquals("https://cdn.example.com/carousel/item2.png", items[1].imageUrl)
        assertNull(items[1].title)
        assertNull(items[1].body)
        assertNull(items[1].action)
        assertEquals(ActionType.OPEN_APP, items[1].actionType)

        // Index 2 has no title
        assertNull(items[2].title)
        assertEquals("index 2 has no title", items[2].body)

        // Indices are unchanged (F5)
        assertEquals("https://www.example.com/offers/0", items[0].action)
        assertEquals("https://www.example.com/offers/3", items[3].action)
    }

    @Test
    fun testItemCountIsNotCapped() {
        val items = carouselFromFixture("seven_items_all_retained")
        assertNotNull(items)
        assertEquals(7, items!!.size)
        items.forEachIndexed { i, item -> assertEquals(i.toString(), item.title) }
    }

    @Test
    fun testActionTypeResolution() {
        val items = carouselFromFixture("action_type_variants")
        assertNotNull(items)
        assertEquals(5, items!!.size)
        assertEquals(ActionType.OPEN_URL, items[0].actionType)
        assertEquals(ActionType.OPEN_APP, items[1].actionType)
        assertEquals("", items[1].action)
        assertEquals(ActionType.OPEN_APP, items[2].actionType) // absent
        assertEquals(ActionType.OPEN_APP, items[3].actionType) // unrecognised
        assertEquals(ActionType.OPEN_APP, items[4].actionType) // open_campaign is out of scope
    }

    @Test
    fun testStraySettingsKeysAreIgnored() {
        val items = carouselFromFixture("stray_settings_keys_are_ignored")
        assertNotNull(items)
        assertEquals(1, items!!.size)
        assertEquals("page control shows regardless", items[0].title)
    }
}
