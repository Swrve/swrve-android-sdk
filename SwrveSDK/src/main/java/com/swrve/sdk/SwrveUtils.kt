package com.swrve.sdk

import android.os.Build
import com.swrve.sdk.messaging.SwrveBaseCampaign.SwrveTimezoneType
import com.swrve.sdk.messaging.SwrveBaseCampaign.SwrveTimezoneType.GLOBAL
import com.swrve.sdk.messaging.SwrveBaseCampaign.SwrveTimezoneType.LOCAL
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Used internally for util helper methods.
 */
class SwrveUtils {
    companion object {
        @JvmStatic
        @Throws(Exception::class)
        fun parseIso8601Date(isoDate: String, timezoneType: SwrveTimezoneType): Date {
            val date: Date
            // Instant.parse requires API level 26 - preference to use this over SimpleDateFormat
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val instant: Instant = when (timezoneType) {
                    GLOBAL -> Instant.parse(isoDate)
                    LOCAL -> {
                        // For local timezone, append the system's timezone offset to the date.
                        val localDateTime = LocalDateTime.parse(isoDate)
                        val zoneOffset = ZoneId.systemDefault().rules.getOffset(localDateTime)
                        localDateTime.toInstant(zoneOffset)
                    }
                }
                date = Date.from(instant)
            } else {
                // Warning - SimpleDateFormat pattern of "yyyy-MM-dd'T'HH:mm:ssX" is not supported pre-nougat Api 24
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                sdf.timeZone = when (timezoneType) {
                    GLOBAL -> TimeZone.getTimeZone("UTC")
                    LOCAL -> TimeZone.getDefault()
                }
                date = sdf.parse(isoDate) ?: throw IllegalArgumentException("SwrveSDK: cannot parse date: $isoDate timezoneType: $timezoneType")
            }
            return date
        }
    }
}
