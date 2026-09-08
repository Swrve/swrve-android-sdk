# Message Center Sample

Listing and managing Message Center in-app campaigns, in Kotlin and Jetpack Compose.

Message Center campaigns are in-app messages the SDK holds back rather than showing automatically, so your app can list them somewhere of its own and let the user open them when they choose.

## Why this exists

The list is not a feed you subscribe to — it is a snapshot you fetch. Campaigns appear once their assets have downloaded, they are filtered by device orientation, and marking one seen or removing it changes what the *next* fetch returns rather than mutating the list you are holding. Miss that and the UI silently stops matching the SDK's state.

## Setup

1. Open `MessageCenterSample` in Android Studio (a standalone project with its own Gradle wrapper — open this directory, not the SDK repo root).
2. In [`SampleApplication.kt`](app/src/main/java/com/swrve/sdk/sample/messagecenter/SampleApplication.kt), replace `YOUR_APP_ID` and `YOUR_API_KEY` with your own, from the Swrve dashboard under **Settings → Integration settings**.
3. Run the app.

No Firebase project or `google-services.json` is needed — Message Center does not involve push, so this sample uses the `swrve` core artifact rather than `swrve-firebase`.

To see anything in the list, create an in-app campaign with **Message Center** enabled and make sure it targets your test device.

## Key API calls

Everything is in [`MainActivity.kt`](app/src/main/java/com/swrve/sdk/sample/messagecenter/MainActivity.kt) — read that file for the working version.

**Fetch the list**, filtered to in-app campaigns for the current orientation:

```kotlin
val campaigns = SwrveSDK.getInAppMessageCenterCampaigns(orientation, null)
```

**Act on a campaign** — display it, mark it seen without displaying, or remove it. Each takes either the campaign object or its id; this sample uses ids, because it keeps the mutable SDK objects out of its UI state (see the last point below):

```kotlin
SwrveSDK.getMessageCenterCampaign(id, null)?.let { SwrveSDK.showMessageCenterCampaign(it) }
SwrveSDK.markMessageCenterCampaignAsSeen(id)
SwrveSDK.removeMessageCenterCampaign(id)
```

**Be told when campaigns change** — including once their assets have downloaded, and whenever a later content refresh brings new ones in:

```kotlin
// Owned here, not by the SDK: its reference is weak, so an inline listener stops firing once collected.
private val campaignsUpdateListener = SwrveCampaignsUpdateListener { /* re-read the list */ }

SwrveSDK.setCampaignsUpdateListener(campaignsUpdateListener)
```

The reference is weak so that registering a listener cannot keep your screen alive after it should have gone.
The trade is that ownership is yours: holding the listener in a property ties it to the screen's lifetime, and
nothing warns you if you don't — the callbacks just stop.

Later calls mean the campaigns *may* have changed — the SDK does not compare against what you last read, so treat every call as "read again". It covers every campaign surface, not just Message Center.

The initial notification is sent once and never replayed, so registering it where the SDK is created is safest. This sample registers in the Activity instead, and gets away with it because the Activity re-reads on its own in `onResume` — so a missed first notification costs nothing here. It also reports SDK-driven changes only: your own `markMessageCenterCampaignAsSeen` and `removeMessageCenterCampaign` calls change what the getters return without firing it, which is why the re-fetch note below matters.

It says to read again — not that you must redraw immediately. This sample re-reads straight away, which is the simplest thing to show, but a list someone is mid-scroll through is a different case: rows appearing or vanishing underneath them is jarring. Setting a flag and offering a "new messages" affordance, then re-reading when the user acts, is equally valid.

Note this happens without any user gesture. The SDK refreshes on its own schedule, so the callback can arrive while the screen is simply open — pull-to-refresh below is a separate, user-initiated path.

**Fetch new content on demand** — wired to pull-to-refresh in this sample — then re-read the list when it completes:

```kotlin
SwrveSDK.refreshContent { result ->
    // re-fetch the list, posting to the UI thread
}
```

## Good to know

- **A campaign is not listable until its assets have downloaded.** That happens *after* the content response arrives, so fetching once and reading immediately finds nothing on a fresh install. `setCampaignsUpdateListener` exists for this: it fires once the SDK has finished attempting those downloads, so there is nothing to poll or delay. Individual downloads can still fail, so re-read the list rather than assuming every campaign is now present.
- **Re-fetch after every action.** The campaigns you hold are a snapshot. `markMessageCenterCampaignAsSeen` and `removeMessageCenterCampaign` change what the next `getInAppMessageCenterCampaigns` call returns; they do not update your list.
- **`showMessageCenterCampaign` already marks the campaign seen.** The separate `markMessageCenterCampaignAsSeen` exists for marking one seen *without* displaying it — the two are not a pair you need to call together.
- **Orientation filters the results.** A campaign with no format for the current orientation is omitted, so passing the wrong value makes campaigns look missing. This sample derives it from the device and re-fetches on rotation.
- **Do not hold campaign objects as UI state.** Marking a campaign seen mutates the campaign in place, and the SDK returns the same instances each time — so a re-read yields a new list of identical references and a Compose (or `DiffUtil`) comparison sees no change. The status updates in the SDK and never reaches the screen. This sample copies the values it needs into an immutable `MessageCenterItem` instead.
- **This sample covers in-app campaigns only.** `getInAppMessageCenterCampaigns` deliberately excludes embedded campaigns; for those see [SwrveEmbeddedSample](../SwrveEmbeddedSample/).
- **Not every field is shown.** Subject and description are optional, so the sample falls back to the campaign name. `SwrveMessageCenterDetails` also carries `imageURL` and `imageSha`, which this sample does not render.
- **Credentials here are sample-grade.** Hardcoding an API key in source is fine for a sample and wrong for a real app.


