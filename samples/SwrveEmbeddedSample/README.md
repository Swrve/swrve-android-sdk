# Swrve Embedded Sample

This sample app demonstrates how to consume **Swrve Embedded Campaigns** and render diverse UI components from a *single* embedded content feed. It focuses on:

* Carousel of promotional cards (`type: carousel`) from Message Center
* Vertical offers list (`type: offer`) supporting multiple layouts (`tall_card`, `compact_card`) from Message Center
* Fixed (inline) banner triggered by a custom event (`type: banner`)
* Floating collapsible banner (`type: floating_banner`) with expand / collapse animation from Message Center

---
## Why Embedded Campaigns?
Embedded campaigns let you design & target content server‑side (in Swrve) while retaining **full client rendering control**. They are ideal when:
* You already have bespoke UI components / design system.
* You want AB‑testable content without shipping an app update.
* You need a mixture of layouts (carousel, banners, contextual offers) powered by one remote feed.

---
## Setup Screen (Offline vs Live Mode)
The sample ships with a built‑in offline data. This lets you explore UI components immediately **without** connecting to Swrve servers.

Open the Setup sheet ("Setup" button on the main menu) to switch between modes or provide credentials.

### Modes
* **Offline** (default on first run): Uses only the bundled JSON fixture. Network calls to fetch embedded campaigns are skipped.
* **Live**: Uses the stored App ID / API Key (and regional stack) to fetch real campaigns & RTUP‑driven personalization.

### Connecting to Swrve Servers
1. Setup → switch to Live.
2. Enter App ID, API Key, select Stack (US / EU / FS).
3. Tap Save. (Sample reads credentials only at launch.)
4. Relaunch once to apply. In production you initialize the SDK once at app start—no runtime "reload" pattern recommended.

### Storage & Security Note
Credentials are stored using a lightweight sample store and `SharedPreferences` for stack. This is **not** production‑grade secret storage—use Android Keystore or a secure config provider for real apps.

---
## Data Flow Overview
1. Call `SwrveSDK.getEmbeddedMessageCenterCampaigns()` to retrieve current embedded campaigns.
2. For each `SwrveEmbeddedMessage` (filtered to embedded type): decode its `data` JSON (string → model).
3. Filter by payload `type` (e.g. `carousel`, `offer`, ...).
4. Sort or prioritize (e.g. `display_order`).
5. Render Jetpack Compose view(s).
6. Track impressions (`embeddedMessageWasShownToUser`) exactly once per message ID.
7. Track CTA taps (`embeddedMessageButtonWasPressed`).

---
## Campaign JSON Structure (inside `data` JSON)
| Field | Type | Notes |
|-------|------|------|
| `type` | string | One of: `carousel`, `offer`, `banner`, `floating_banner` |
| `version` | number (optional) | Schema version (default assumed 1). Payload ignored if `version` > `EMBEDDED_SCHEMA_VERSION` |
| `display_order` | number (optional) | Ascending priority (lower value shown first). Missing => sorted last |
| `title` | `{ text, color }` (optional) | Omitted for purely image layouts |
| `body` | `{ text, color }` (optional) | Optional secondary text; never raw string anymore |
| `image` | string URL (optional) | Required for carousel / most offers layouts; optional for some banners |
| `background_color` | hex string (optional) | Applied to card / banner surface; fallback theme color if absent |
| `layout` | string (optional) | Layout dispatch token (e.g. `tall_card`, `compact_card`, `left_aligned_banner`) |
| `location` | string (optional) | For floating banners: `top` or `bottom` (default: `top`) |
| `cta` | `{ text, url, color, background_color }` (optional) | CTA text may be omitted if only a tappable surface is desired |

CTA object fields:
| CTA Field | Meaning |
|-----------|---------|
| `text` | Button label; if empty while `url` present, no button is drawn (surface may still handle taps) |
| `url` | Destination deep link / web link |
| `color` | Label (foreground) color override |
| `background_color` | Button background color override |

### Type-Specific Schemas
#### Carousel (`type: carousel`)
Layouts: `tall_card` (image + reserved text band) or `image_only_card` (just image). Image ratio enforced at 2:1.

#### Offer (`type: offer`)
Layouts: `tall_card` (image on top) or `compact_card` (square thumbnail + text column). CTA optional; card taps still possible.

#### Banner (`type: banner`)
Fields include `layout` variants: `left_aligned_banner`, `centered_banner`, `right_aligned_banner`. Uses `image` plus overlay text & CTA.

#### Floating Banner (`type: floating_banner`)
Adds:
* `location`: `top` | `bottom` (determines screen position and slide animation direction)
* `image`: optional image icon URL
* `expanded_title`, `expanded_body`, `helper_text`: text fields for expanded state
* Collapsed state shows `title` and `body`
* CTA appears only in expanded state

### Schema Versioning
The sample defines a constant `EMBEDDED_SCHEMA_VERSION = 1` inside the models file. Incoming payloads include an optional `version` field. If omitted it is treated as `1`. Any payload whose `version` exceeds the client constant is **skipped silently** (future-proofing against incompatible server changes). Add logging if you need visibility into skipped future versions.

---
## Decoding Pattern
`SwrveEmbeddedMessage.data` arrives as an optional `String`. Decode directly into the appropriate model using Kotlin's `Json.decodeFromString`. Apply version gating before using the object:

```kotlin
val dataString = message.getData()
if (dataString != null) {
    try {
        val data = Json.decodeFromString<EmbeddedData>(dataString)
        if ((data.version ?: 1) <= EMBEDDED_SCHEMA_VERSION) {
            // handle carousel / offer / banner based on data.type
        }
    } catch (e: Exception) {
        // Handle decode failure
    }
}
```

Each view (carousel, offers, floating banner) follows the same guard pattern using Kotlin's `try-catch` for safe deserialization.

---
## Filtering & Ordering
* **Carousel / Offers**: Keep only payloads whose `type` matches and required fields (image, layout) are present; sort by `display_order` ascending.
* **Floating Banner**: Pick highest priority (lowest `display_order`); tie‑break by most recent campaign download date.
* **Banner**: Appears only when a custom event (`banner`) triggers delivery; it listens for embedded message callbacks.

---

## Impression & CTA Tracking
| Event | Call |
|-------|------|
| Impression (first display) | `SwrveSDK.embeddedMessageWasShownToUser(message)` |
| CTA / button press | `SwrveSDK.embeddedMessageButtonWasPressed(message, buttonName)` |

The sample uses `LaunchedEffect` with message ID dependency to ensure impressions fire when content *actually becomes visible*.
A set-based approach prevents duplicate impression reporting.

---
## Triggered (Inline) Fixed Banner Flow
1. App fires `SwrveSDK.event("banner")`.
2. SDK returns/pushes eligible embedded message via callback.
3. Payload decoded if it matches `type: banner` schema.
4. Impression recorded upon first render.

---
## Running the Sample
Steps:
1. Open project in Android Studio.
2. Sync Gradle dependencies.
3. Build & run on emulator or device.
4. Main menu opens with buttons to Carousel, Offers, Fixed Banner, Floating Banner, etc.
5. Setup button allows to switch on live campaigns by entering credentials, otherwise offline mode loads off the shelf content.
6. Message center driven campaigns triggered via Carousel, Offers, Floating Banner buttons.
7. Event driven (`event=banner`) campaign triggered via Fixed Banner button.

---
## License / Usage
Provided for demonstration purposes. Reuse patterns (ordering, impression dedupe, flexible layout dispatch) in accordance with your internal guidelines and Swrve SDK terms.
