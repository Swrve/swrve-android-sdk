package com.swrve.sdk.sample.embedded

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.messaging.SwrveEmbeddedMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

data class EmbeddedText(val text: String?, val color: String?)
data class EmbeddedCTA(
    val text: String?,
    val url: String?,
    val color: String?,
    val backgroundColor: String?
)

data class EmbeddedData(
    val type: String?,
    val displayOrder: Int?,
    val title: EmbeddedText?,
    val body: EmbeddedText?,
    val image: String?,
    val backgroundColor: String?,
    val layout: String?,
    val cta: EmbeddedCTA?,
    val version: Int?,
    val expandedTitle: EmbeddedText? = null,
    val expandedBody: EmbeddedText? = null,
    val helperText: EmbeddedText? = null,
    val location: String? = null
)

data class EmbeddedEntry(val data: EmbeddedData, val message: SwrveEmbeddedMessage)

private const val EMBEDDED_SCHEMA_VERSION = 1

class EmbeddedViewModel(app: Application) : AndroidViewModel(app) {

    private val _embeddedData = MutableStateFlow<List<EmbeddedEntry>>(emptyList())
    val embeddedData: StateFlow<List<EmbeddedEntry>> = _embeddedData
    private val _fixedBanner = MutableStateFlow<EmbeddedEntry?>(null)
    val fixedBanner: StateFlow<EmbeddedEntry?> = _fixedBanner
    private val _offers = MutableStateFlow<List<EmbeddedEntry>>(emptyList())
    val offers: StateFlow<List<EmbeddedEntry>> = _offers
    private val _floatingBanner = MutableStateFlow<EmbeddedEntry?>(null)
    val floatingBanner: StateFlow<EmbeddedEntry?> = _floatingBanner

    init {
        // Collect triggered embedded messages (banner) from the application-scoped bus
        viewModelScope.launch {
            (getApplication() as SampleApplication).fixedBannerFlow.collect { evt ->
                try {
                    val embeddedMessage = evt["message"] as? SwrveEmbeddedMessage ?: return@collect
                    val raw = (evt["data"] as? String) ?: embeddedMessage.data
                    val parsedBannerData = parseBannerData(raw)
                    if (parsedBannerData != null) {
                        _fixedBanner.emit(EmbeddedEntry(parsedBannerData, embeddedMessage))
                    }
                } catch (e: Exception) {
                    Timber.tag("EmbeddedSample").e(e, "Error loading fixed banner campaign")
                }
            }
        }
    }

    fun loadCarousel() {
        viewModelScope.launch {
            try {
                val embeddedMessages = if (isOfflineMode()) {
                    DemoCampaignsManager().getDemoCampaigns(getApplication())
                } else {
                    SwrveSDK.getEmbeddedMessageCenterCampaigns()
                }
                val result = mutableListOf<EmbeddedEntry>()
                for (embedded in embeddedMessages) {
                    if (embedded.data.isNullOrEmpty() || embedded.type != SwrveEmbeddedMessage.EMBEDDED_CAMPAIGN_TYPE.JSON) continue
                    parseCarouselData(embedded.data)?.let { parsed ->
                        result.add(EmbeddedEntry(data = parsed, message = embedded))
                    }
                }
                _embeddedData.value = result.sortedBy { it.data.displayOrder ?: Int.MAX_VALUE }
            } catch (e: Exception) {
                Timber.tag("EmbeddedSample").e(e, "Error loading carousel campaigns")
                _embeddedData.value = emptyList()
            }
        }
    }

    fun loadOffers() {
        viewModelScope.launch {
            try {
                val embeddedMessages = if (isOfflineMode()) {
                    DemoCampaignsManager().getDemoCampaigns(getApplication())
                } else {
                    SwrveSDK.getEmbeddedMessageCenterCampaigns()
                }
                val result = mutableListOf<EmbeddedEntry>()
                for (embedded in embeddedMessages) {
                    if (embedded.data.isNullOrEmpty() || embedded.type != SwrveEmbeddedMessage.EMBEDDED_CAMPAIGN_TYPE.JSON) continue
                    parseOfferData(embedded.data)?.let { parsed ->
                        result.add(EmbeddedEntry(data = parsed, message = embedded))
                    }
                }
                _offers.value = result.sortedBy { it.data.displayOrder ?: Int.MAX_VALUE }
            } catch (e: Exception) {
                Timber.tag("EmbeddedSample").e(e, "Error loading offer campaigns")
                _offers.value = emptyList()
            }
        }
    }

    fun loadFloatingBanner() {
        viewModelScope.launch {
            try {
                val embeddedMessages = if (isOfflineMode()) {
                    DemoCampaignsManager().getDemoCampaigns(getApplication())
                } else {
                    SwrveSDK.getEmbeddedMessageCenterCampaigns()
                }
                val candidates = embeddedMessages.mapNotNull { msg ->
                    if (msg.data.isNullOrEmpty() || msg.type != SwrveEmbeddedMessage.EMBEDDED_CAMPAIGN_TYPE.JSON) return@mapNotNull null
                    parseFloatingBannerData(msg.data)?.let { parsed -> EmbeddedEntry(parsed, msg) }
                }

                if (candidates.isEmpty()) {
                    _floatingBanner.value = null
                } else {
                    val ordered = candidates.sortedWith(compareBy<EmbeddedEntry> {
                        it.data.displayOrder ?: Int.MAX_VALUE
                    }.thenByDescending {
                        it.message.campaign?.downloadDate ?: java.util.Date(0)
                    })
                    _floatingBanner.value = ordered.first()
                }
            } catch (e: Exception) {
                Timber.tag("EmbeddedSample").e(e, "Error loading floating banner campaigns")
                _floatingBanner.value = null
            }
        }
    }

    /** Trigger the banner event after collector has started to avoid race conditions. */
    fun requestBanner() {
        viewModelScope.launch {
            delay(400)
            val name = "banner"
            if (isOfflineMode()) {
                triggerDemoBanner(name)
            } else {
                SwrveSDK.event(name)
            }
        }
    }

    fun addToCart(index: Int) {
        viewModelScope.launch {
            val name = "add_to_cart_product_${index}"
            if (isOfflineMode()) {
                triggerDemoBanner(name)
            } else {
                SwrveSDK.event(name)
            }
        }
    }

    private fun parseBannerData(raw: String?): EmbeddedData? {
        if (raw.isNullOrEmpty()) return null
        val obj = JSONObject(raw)
        val isBanner = obj.optString("type").equals("banner", ignoreCase = true)
        if (!isBanner) return null
        val layout = obj.optString("layout")
        val titleObj = obj.optJSONObject("title")
        val bodyObj = obj.optJSONObject("body")
        val ctaObj = obj.optJSONObject("cta")
        return EmbeddedData(
            type = obj.optString("type"),
            displayOrder = obj.optInt("display_order"),
            title = EmbeddedText(titleObj?.optString("text"), titleObj?.optString("color")),
            body = EmbeddedText(bodyObj?.optString("text"), bodyObj?.optString("color")),
            image = if (obj.has("image")) obj.optString("image") else null,
            backgroundColor = if (obj.has("background_color")) obj.optString("background_color") else null,
            layout = layout,
            cta = EmbeddedCTA(
                text = ctaObj?.optString("text"),
                url = ctaObj?.optString("url"),
                color = ctaObj?.optString("color"),
                backgroundColor = ctaObj?.optString("background_color")
            ),
            version = obj.optInt("version", 1)
        )
    }

    private fun parseCarouselData(raw: String?): EmbeddedData? {
        if (raw.isNullOrEmpty()) return null
        val obj = JSONObject(raw)
        val isCarousel = obj.optString("type").equals("carousel", ignoreCase = true)
        if (!isCarousel) return null
        val version = obj.optInt("version", 1)
        if (version > EMBEDDED_SCHEMA_VERSION) return null
        val layout = obj.optString("layout")
        if (layout != "tall_card" && layout != "image_only_card") return null
        val titleObj = obj.optJSONObject("title")
        val bodyObj = obj.optJSONObject("body")
        val ctaObj = obj.optJSONObject("cta")
        return EmbeddedData(
            type = obj.optString("type"),
            displayOrder = obj.optInt("display_order"),
            title = EmbeddedText(titleObj?.optString("text"), titleObj?.optString("color")),
            body = EmbeddedText(bodyObj?.optString("text"), bodyObj?.optString("color")),
            image = if (obj.has("image")) obj.optString("image") else null,
            backgroundColor = if (obj.has("background_color")) obj.optString("background_color") else null,
            layout = layout,
            cta = EmbeddedCTA(
                text = ctaObj?.optString("text"),
                url = ctaObj?.optString("url"),
                color = ctaObj?.optString("color"),
                backgroundColor = ctaObj?.optString("background_color")
            ),
            version = version
        )
    }

    private fun parseOfferData(raw: String?): EmbeddedData? {
        if (raw.isNullOrEmpty()) return null
        val obj = JSONObject(raw)
        val isOffer = obj.optString("type").equals("offer", ignoreCase = true)
        if (!isOffer) return null
        val version = obj.optInt("version", 1)
        if (version > EMBEDDED_SCHEMA_VERSION) return null
        val layout = obj.optString("layout").lowercase()
        if (layout != "tall_card" && layout != "compact_card") return null
        val titleObj = obj.optJSONObject("title")
        val bodyObj = obj.optJSONObject("body")
        val ctaObj = obj.optJSONObject("cta")
        val image = if (obj.has("image")) obj.optString("image") else null
        if (image.isNullOrEmpty()) return null
        return EmbeddedData(
            type = obj.optString("type"),
            displayOrder = obj.optInt("display_order"),
            title = EmbeddedText(titleObj?.optString("text"), titleObj?.optString("color")),
            body = EmbeddedText(bodyObj?.optString("text"), bodyObj?.optString("color")),
            image = image,
            backgroundColor = if (obj.has("background_color")) obj.optString("background_color") else null,
            layout = layout,
            cta = EmbeddedCTA(
                text = ctaObj?.optString("text"),
                url = ctaObj?.optString("url"),
                color = ctaObj?.optString("color"),
                backgroundColor = ctaObj?.optString("background_color")
            ),
            version = version
        )
    }

    private fun parseFloatingBannerData(raw: String?): EmbeddedData? {
        if (raw.isNullOrEmpty()) return null
        val obj = JSONObject(raw)
        val isFloating = obj.optString("type").equals("floating_banner", ignoreCase = true)
        if (!isFloating) return null
        val version = obj.optInt("version", 1)
        if (version > EMBEDDED_SCHEMA_VERSION) return null

        val collapsed = obj.optJSONObject("collapsed")
        val collapsedTitleObj = collapsed?.optJSONObject("title")
        val collapsedBodyObj = collapsed?.optJSONObject("body")

        val expanded = obj.optJSONObject("expanded")
        val expandedTitleObj = expanded?.optJSONObject("title")
        val expandedBodyObj = expanded?.optJSONObject("body")
        val helperObj = expanded?.optJSONObject("helper_text")
        val ctaObj = expanded?.optJSONObject("cta")

        return EmbeddedData(
            type = obj.optString("type"),
            displayOrder = obj.optInt("display_order"),
            title = EmbeddedText(collapsedTitleObj?.optString("text"), collapsedTitleObj?.optString("color")),
            body = EmbeddedText(collapsedBodyObj?.optString("text"), collapsedBodyObj?.optString("color")),
            image = if (obj.has("image")) obj.optString("image") else null,
            backgroundColor = if (obj.has("background_color")) obj.optString("background_color") else null,
            layout = "collapsed_to_expanded",
            cta = EmbeddedCTA(
                text = ctaObj?.optString("text"),
                url = ctaObj?.optString("url"),
                color = ctaObj?.optString("color"),
                backgroundColor = ctaObj?.optString("background_color")
            ),
            version = version,
            expandedTitle = EmbeddedText(expandedTitleObj?.optString("text"), expandedTitleObj?.optString("color")),
            expandedBody = EmbeddedText(expandedBodyObj?.optString("text"), expandedBodyObj?.optString("color")),
            helperText = EmbeddedText(helperObj?.optString("text"), helperObj?.optString("color")),
            location = obj.optString("location", "top")
        )
    }

    private fun triggerDemoBanner(name: String) {
        viewModelScope.launch {
            delay(500)
            val embeddedMessage = DemoCampaignsManager().getDemoCampaign(getApplication(), name)
            if (embeddedMessage == null) {
                return@launch
            }
            try {
                val raw = embeddedMessage.dataRaw
                val parsedBannerData = parseBannerData(raw)
                if (parsedBannerData != null) {
                    _fixedBanner.emit(EmbeddedEntry(parsedBannerData, embeddedMessage))
                }
            } catch (e: Exception) {
                Timber.tag("EmbeddedSample").e(e, "Error emitting add_to_cart banner from demo campaigns")
            }
        }
    }

    private fun isOfflineMode(): Boolean {
        val prefs = SwrvePrefs.prefs(getApplication())
        return SwrvePrefs.isOffline(prefs)
    }
}
