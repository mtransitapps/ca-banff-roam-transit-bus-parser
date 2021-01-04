package org.mtransit.parser.ca_banff_roam_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// http://www.banffopendata.ca/
// https://roamtransit.com/wp-content/uploads/GTFS/GTFS.zip
// http://data.trilliumtransit.com/gtfs/roamtransit-banff-ab-ca/roamtransit-banff-ab-ca.zip
public class BanffRoamTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-banff-roam-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new BanffRoamTransitBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating Roam Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating Roam Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String B = "b";
	private static final String S = "s";
	private static final String X = "x";

	private static final long RID_ENDS_WITH_B = 2_000L;
	private static final long RID_ENDS_WITH_S = 19_000L;
	private static final long RID_ENDS_WITH_X = 24_000L;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		final String rsn = gRoute.getRouteShortName();
		if (Utils.isDigitsOnly(rsn)) {
			return Long.parseLong(rsn);
		}
		if ("on-it".equalsIgnoreCase(rsn)) {
			return 10_981L;
		}
		if ("Moraine Lake Shuttle".equalsIgnoreCase(rsn)) {
			return 16_714L;
		}
		if ("1 & 2".equalsIgnoreCase(rsn)) {
			return 10_002L;
		}
		final Matcher matcher = DIGITS.matcher(rsn);
		if (matcher.find()) {
			final int digits = Integer.parseInt(matcher.group());
			final String rsnLC = rsn.toLowerCase(Locale.ENGLISH);
			if (rsnLC.endsWith(B)) {
				return RID_ENDS_WITH_B + digits;
			} else if (rsnLC.endsWith(S)) {
				return RID_ENDS_WITH_S + digits;
			} else if (rsnLC.endsWith(X)) {
				return RID_ENDS_WITH_X + digits;
			}
		}
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute.toStringPlus());
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		if (gRoute.getRouteShortName().equalsIgnoreCase("Moraine Lake Shuttle")) {
			return "MLS";
		}
		return super.getRouteShortName(gRoute);
	}

	private static final Pattern STARTS_WITH_ROUTE_RID = Pattern.compile("(route [0-9]+[a-z]?( & [0-9]+)? (- )?)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = STARTS_WITH_ROUTE_RID.matcher(routeLongName).replaceAll(Constants.EMPTY);
		routeLongName = CleanUtils.CLEAN_AND.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return routeLongName;
	}

	private static final String AGENCY_COLOR_DARK_GREY = "231F20"; // DARK GREY (from PNG logo)

	private static final String AGENCY_COLOR = AGENCY_COLOR_DARK_GREY;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern HIGH_SCHOOL_ = CleanUtils.cleanWords("high school");
	private static final String HIGH_SCHOOL_REPLACEMENT = CleanUtils.cleanWordsReplacement("HS");

	private static final Pattern TRANSIT_HUB_ = CleanUtils.cleanWords("transit hub");

	@NotNull
	@Override
	public String cleanDirectionHeadsign(boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = HIGH_SCHOOL_.matcher(directionHeadSign).replaceAll(HIGH_SCHOOL_REPLACEMENT);
		directionHeadSign = TRANSIT_HUB_.matcher(directionHeadSign).replaceAll(Constants.EMPTY);
		return super.cleanDirectionHeadsign(fromStopName, directionHeadSign);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
