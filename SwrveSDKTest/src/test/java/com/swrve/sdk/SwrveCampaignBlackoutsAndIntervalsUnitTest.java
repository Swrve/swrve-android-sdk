package com.swrve.sdk;

import static com.swrve.sdk.SwrveTrackingState.STARTED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;

import com.swrve.sdk.messaging.SwrveBaseCampaign;
import com.swrve.sdk.messaging.SwrveBaseMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

public class SwrveCampaignBlackoutsAndIntervalsUnitTest extends SwrveBaseTest {

    @Rule
    public RetryRule retryRule = new RetryRule(3); // Retry up to 3 times

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        swrveSpy = SwrveTestUtils.createSpyInstance();
        SwrveTestUtils.disableRestClientExecutor(swrveSpy);
        swrveSpy.profileManager.setTrackingState(STARTED);
        swrveSpy.onActivityCreated(mActivity, null);
        SwrveTestUtils.flushLifecycleExecutorQueue(swrveSpy);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        Mockito.reset(swrveSpy);
    }

    @Test
    public void testCampaignWithLocalDates_CheckStart() throws Exception {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // Begin with setting the Robolectric timezone to UTC

        // Set time now to one second into 12th September 2024
        Date dateUTC = Date.from(Instant.parse("2024-09-12T00:00:01Z")); // Ensure Z is added for UTC
        doReturn(dateUTC).when(swrveSpy).getNow();
        swrveSpy.initialisedTime = dateUTC;

        // load campaign that is valid for one day only 2024-09-12
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_local_dates.json", "dummyAsset");

        // Local time is same as UTC time so it should be valid
        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);

        // Set local time (Robolectric timezone) to EST, which is 4 hours behind UTC, so it should not be started
        TimeZone.setDefault(TimeZone.getTimeZone("EST")); // Eastern Standard Time
        message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);

        // Set local time (Robolectric timezone) to Pacific/Auckland, which is 12 hours ahead of UTC, so it should be valid
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland")); // New Zealand
        message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);
    }

    @Test
    public void testCampaignWithLocalDates_CheckEnd() throws Exception {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // Begin with setting the Robolectric timezone to UTC

        // Set time now to end of 12th September 2024
        Date dateUTC = Date.from(Instant.parse("2024-09-12T23:59:59Z")); // Ensure Z is added for UTC
        doReturn(dateUTC).when(swrveSpy).getNow();
        swrveSpy.initialisedTime = dateUTC;

        // load campaign that is valid for one day only 2024-09-12
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_local_dates.json", "dummyAsset");

        // Local time is same as UTC time so it should be valid
        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);

        // Set local time (Robolectric timezone) to EST, which is 4 hours behind UTC, so it should be invalid
        TimeZone.setDefault(TimeZone.getTimeZone("EST"));
        message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNotNull(message);

        // Set local time (Robolectric timezone) to Pacific/Auckland, which is 12 hours ahead of UTC, so it should be finished
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland")); // New Zealand
        message = swrveSpy.getBaseMessageForEvent("Swrve.currency_given");
        assertNull(message);
    }

    @Test
    public void testBlackoutDates() throws Exception {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // Begin with setting the Robolectric timezone to UTC

        // Set time now to 10th September 2024
        Date dateUTC = Date.from(Instant.parse("2024-09-10T12:00:00Z")); // Ensure Z is added for UTC
        doReturn(dateUTC).when(swrveSpy).getNow();
        swrveSpy.initialisedTime = dateUTC;

        // load campaign that has blackout dates 2024-09-11 and 2024-09-14
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_blackout_dates.json", "dummyAsset");

        // Current date is 10th so it should be valid
        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("trigger.100");
        assertNotNull(message);

        // Set time now to 11th so its in the blackout date range
        dateUTC = Date.from(Instant.parse("2024-09-11T12:00:00Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.100");
        assertNull(message);

        // Set time now to 12th so its out of the blackout date range
        dateUTC = Date.from(Instant.parse("2024-09-12T12:00:00Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.100");
        assertNotNull(message);

        // Set time now to 13th so its in the blackout date range
        dateUTC = Date.from(Instant.parse("2024-09-13T12:00:00Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.100");
        assertNull(message);

        // Set time now to 14th so its out of the blackout date range
        dateUTC = Date.from(Instant.parse("2024-09-14T12:00:00Z")); // Ensure Z is added for UTC
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.100");
        assertNotNull(message);
    }

    @Test
    public void testIntervalTimes_global() throws Exception {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // Begin with setting the Robolectric timezone to UTC

        // Set time now to 10th September 2024 8:59:59 UTC
        Date dateUTC = Date.from(Instant.parse("2024-09-10T08:59:59Z")); // Ensure Z is added for UTC
        doReturn(dateUTC).when(swrveSpy).getNow();
        swrveSpy.initialisedTime = dateUTC;

        // load campaign from "trigger.global" that has interval times 09:00:00-13:00:00 and 14:00:00-17:30:00
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_interval_times.json", "dummyAsset");

        // Current date is 10th September 2024 8:59:59 UTC so it should be outside the interval
        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNull(message);

        // Set time now to 10th at 09:00:01 so its just inside the interval
        dateUTC = Date.from(Instant.parse("2024-09-10T09:00:01Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNotNull(message);

        // Set time now to 10th at 12:59:59 so its just inside the interval
        dateUTC = Date.from(Instant.parse("2024-09-10T12:59:59Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNotNull(message);

        // Set time now to 10th at 13:00:01 so its just outside the interval
        dateUTC = Date.from(Instant.parse("2024-09-10T13:00:01Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNull(message);

        // Set time now to 10th at 13:59:59 so its just outside the interval
        dateUTC = Date.from(Instant.parse("2024-09-10T13:59:59Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNull(message);

        // Set time now to 10th at 14:00:01 so its just inside the interval
        dateUTC = Date.from(Instant.parse("2024-09-10T14:00:01Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNotNull(message);

        // Set time now to 10th at 17:29:59 so its just inside the interval
        dateUTC = Date.from(Instant.parse("2024-09-10T17:29:59Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNotNull(message);

        // Set time now to 10th at 17:30:01 so its just outside the interval
        dateUTC = Date.from(Instant.parse("2024-09-10T17:30:01Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNull(message);
    }

    @Test
    public void testIntervalTimes_global_different_timezone() throws Exception {

        TimeZone.setDefault(TimeZone.getTimeZone("EST")); // Eastern Standard Time

        Date dateNow = Date.from(LocalDateTime.parse("2024-09-10T04:59:59").atZone(ZoneId.of("America/New_York")).toInstant());

        doReturn(dateNow).when(swrveSpy).getNow();
        swrveSpy.initialisedTime = dateNow;

        // load campaign from "trigger.global" that has interval times 09:00:00-13:00:00 and 14:00:00-17:30:00....which is UTC time!!
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_interval_times.json", "dummyAsset");

        // Current date is 10th September 2024 04:59:59 New York (08:59:59 UTC) so it should be outside the interval
        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNull(message);

        // Set time now to 10th at 05:00:01 New York (09:00:01 UTC) so its just inside the interval
        dateNow = Date.from(LocalDateTime.parse("2024-09-10T05:00:01").atZone(ZoneId.of("America/New_York")).toInstant());
        doReturn(dateNow).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNotNull(message);

        // Set time now to 10th at 08:59:59 New York (12:59:59 UTC) so its just inside the interval
        dateNow = Date.from(LocalDateTime.parse("2024-09-10T08:59:59").atZone(ZoneId.of("America/New_York")).toInstant());
        doReturn(dateNow).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNotNull(message);

        // Set time now to 10th at 09:00:01 New York (13:00:01 UTC) so its just outside the interval
        dateNow = Date.from(LocalDateTime.parse("2024-09-10T09:00:01").atZone(ZoneId.of("America/New_York")).toInstant());
        doReturn(dateNow).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNull(message);

        // Set time now to 10th at 09:59:59 New York (13:59:59 UTC) so its just outside the interval
        dateNow = Date.from(LocalDateTime.parse("2024-09-10T09:59:59").atZone(ZoneId.of("America/New_York")).toInstant());
        doReturn(dateNow).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNull(message);

        // Set time now to 10th at 10:00:01 New York (14:00:01 UTC) so its just inside the interval
        dateNow = Date.from(LocalDateTime.parse("2024-09-10T10:00:01").atZone(ZoneId.of("America/New_York")).toInstant());
        doReturn(dateNow).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNotNull(message);

        // Set time now to 10th at 13:29:59 New York (17:29:59 UTC) so its just inside the interval
        dateNow = Date.from(LocalDateTime.parse("2024-09-10T13:29:59").atZone(ZoneId.of("America/New_York")).toInstant());
        doReturn(dateNow).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNotNull(message);

        // Set time now to 10th at 13:30:01 New York (17:30:01 UTC) so its just outside the interval
        dateNow = Date.from(LocalDateTime.parse("2024-09-10T13:30:01").atZone(ZoneId.of("America/New_York")).toInstant());
        doReturn(dateNow).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.global");
        assertNull(message);
    }

    @Test
    public void testIntervalTimes_local() throws Exception {

        TimeZone.setDefault(TimeZone.getTimeZone("EST")); // Eastern Standard Time

        // Set time now to 10th September 2024 8:59:59
        Date dateLocal = SwrveUtils.parseIso8601Date("2024-09-10T08:59:59", SwrveBaseCampaign.SwrveTimezoneType.LOCAL);
        doReturn(dateLocal).when(swrveSpy).getNow();
        swrveSpy.initialisedTime = dateLocal;

        // load campaign from "trigger.local" that has interval times 09:00:00-13:00:00 and 14:00:00-17:30:00
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_interval_times.json", "dummyAsset");

        // Current date is 10th September 2024 8:59:59 EST so it should be outside the interval
        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("trigger.local");
        assertNull(message);

        // Set time now to 10th at 09:00:01 so its just inside the interval
        dateLocal = SwrveUtils.parseIso8601Date("2024-09-10T09:00:01", SwrveBaseCampaign.SwrveTimezoneType.LOCAL);
        doReturn(dateLocal).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.local");
        assertNotNull(message);

        // Set time now to 10th at 12:59:59 so its just inside the interval
        dateLocal = SwrveUtils.parseIso8601Date("2024-09-10T12:59:59", SwrveBaseCampaign.SwrveTimezoneType.LOCAL);
        doReturn(dateLocal).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.local");
        assertNotNull(message);

        // Set time now to 10th at 13:00:01 so its just outside the interval
        dateLocal = SwrveUtils.parseIso8601Date("2024-09-10T13:00:01", SwrveBaseCampaign.SwrveTimezoneType.LOCAL);
        doReturn(dateLocal).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.local");
        assertNull(message);

        // Set time now to 10th at 13:59:59 so its just outside the interval
        dateLocal = SwrveUtils.parseIso8601Date("2024-09-10T13:59:59", SwrveBaseCampaign.SwrveTimezoneType.LOCAL);
        doReturn(dateLocal).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.local");
        assertNull(message);

        // Set time now to 10th at 14:00:01 so its just inside the interval
        dateLocal = SwrveUtils.parseIso8601Date("2024-09-10T14:00:01", SwrveBaseCampaign.SwrveTimezoneType.LOCAL);
        doReturn(dateLocal).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.local");
        assertNotNull(message);

        // Set time now to 10th at 17:29:59 so its just inside the interval
        dateLocal = SwrveUtils.parseIso8601Date("2024-09-10T17:29:59", SwrveBaseCampaign.SwrveTimezoneType.LOCAL);
        doReturn(dateLocal).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.local");
        assertNotNull(message);

        // Set time now to 10th at 17:30:01 so its just outside the interval
        dateLocal = SwrveUtils.parseIso8601Date("2024-09-10T17:30:01", SwrveBaseCampaign.SwrveTimezoneType.LOCAL);
        doReturn(dateLocal).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.local");
        assertNull(message);
    }

    @Test
    public void testBlackoutDatesAndIntervalsCombined() throws Exception {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // Begin with setting the Robolectric timezone to UTC

        // Set time now to 10th September 2024
        Date dateUTC = Date.from(Instant.parse("2024-09-10T12:00:00Z")); // Ensure Z is added for UTC
        doReturn(dateUTC).when(swrveSpy).getNow();
        swrveSpy.initialisedTime = dateUTC;

        // load campaign that has blackout dates 2024-09-11 and 2024-09-14
        SwrveTestUtils.loadCampaignsFromFile(mActivity, swrveSpy, "campaign_blackouts_and_intervals.json", "dummyAsset");

        // Current date is 10th so it should be valid
        SwrveBaseMessage message = swrveSpy.getBaseMessageForEvent("trigger.100");
        assertNotNull(message);

        // Keep the same date as the 10th so its out the blackout date range, but change the time to 13:01 so its outside the interval
        dateUTC = Date.from(Instant.parse("2024-09-10T13:00:01Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.100");
        assertNull(message);

        // Set time now to 11th so its in the blackout date range
        dateUTC = Date.from(Instant.parse("2024-09-11T12:00:00Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.100");
        assertNull(message);

        // Set time now to 12th so its out of the blackout date range but within the interval
        dateUTC = Date.from(Instant.parse("2024-09-12T12:00:00Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.100");
        assertNotNull(message);

        // Set time now to 12th so its out of the blackout date range, but change the time to 13:01 so its outside the interval
        dateUTC = Date.from(Instant.parse("2024-09-12T13:00:01Z"));
        doReturn(dateUTC).when(swrveSpy).getNow();
        message = swrveSpy.getBaseMessageForEvent("trigger.100");
        assertNull(message);
    }
}
