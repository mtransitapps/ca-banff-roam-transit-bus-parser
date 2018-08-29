package org.mtransit.parser.ca_banff_roam_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// http://www.banffopendata.ca/
// http://roamtransit.com/wp-content/uploads/2017/09/GTFS.zip
public class BanffRoamTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-banff-roam-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new BanffRoamTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating Roam Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating Roam Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		return super.excludeRoute(gRoute);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String B = "b";

	private static final long RID_ENDS_WITH_B = 2_000L;

	@Override
	public long getRouteId(GRoute gRoute) {
		if (Utils.isDigitsOnly(gRoute.getRouteShortName())) {
			return Long.parseLong(gRoute.getRouteShortName());
		}
		if ("On-it".equals(gRoute.getRouteShortName())) {
			return 10_981L;
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			if (gRoute.getRouteShortName().endsWith(B)) {
				return RID_ENDS_WITH_B + digits;
			}
		}
		System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final Pattern STARTS_WITH_ROUTE_RID = Pattern.compile("(route [0-9]{1} )", Pattern.CASE_INSENSITIVE);

	private static final Pattern AND = Pattern.compile("((^|\\W){1}(and)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String AND_REPLACEMENT = "$2&$4";

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = STARTS_WITH_ROUTE_RID.matcher(routeLongName).replaceAll(StringUtils.EMPTY);
		routeLongName = AND.matcher(routeLongName).replaceAll(AND_REPLACEMENT);
		routeLongName = CleanUtils.cleanStreetTypes(routeLongName);
		return routeLongName;
	}

	private static final String AGENCY_COLOR_DARK_GREY = "231F20"; // DARK GREY (from PNG logo)

	private static final String AGENCY_COLOR = AGENCY_COLOR_DARK_GREY;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}


	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1L, new RouteTripSpec(1L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Downtown", // UPTOWN_BANFF, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Banff Gondola") // SULPHUR_MTN) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2434984", // Banff Gondola
								"2428690", // "13", // Rimrock Resort Hotel
								"2428681", // "22" // Inns of Banff
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428701", // "2", // Marmot Cresent
								"2428692", // "11", // Banff Upper Hot Springs
								"2434984", // Banff Gondola
						})) //
				.compileBothTripSort());
		map2.put(2L, new RouteTripSpec(2L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Campground", // TUNNEL_MTN, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Fairmont Hotel") // BANFF_SPGS) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428673", // "37", // Fairmont Banff Springs Hotel
								"2428670", // "43" // Douglas Fir Resort
								"2435111", // "30", // Tunnel Mountain Campground
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2435111", // "30", // Tunnel Mountain Campground
								"2428677", // "31", // Hidden Ridge Resort
								"2428673", // "37" // Fairmont Banff Springs Hotel
						})) //
				.compileBothTripSort());
		map2.put(3L, new RouteTripSpec(3L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Banff", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Canmore") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428657", // "109", // Canmore Benchlands Overpass South
								"2428656", // "110", // Canmore Holiday Inn
								"2428685", // "18" // Banff High School
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428685", // "18", // Banff High School
								"2428661", // "105", // Canmore Collegiate
								"2428657", // "109" // Canmore Benchlands Overpass South
						})) //
				.compileBothTripSort());
		map2.put(4L, new RouteTripSpec(4L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Banff Train Sta", // DOWNTOWN_BANFF, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Cave & Basin") // SULPHUR_MTN
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428691", // "12",// Banff Train Sta
								"2428685", // "18" // Banff High School
								"2428667", // Recreation Grounds Entrance
								"2428666", // Cave and Basin
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428666", // Cave and Basin
								"2428697", // "6", // Downtown Wolf St West
								"2428691", // "12" // Banff Train Sta
						})) //
				.compileBothTripSort());
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Glacier Dr", // "Bow Mdws Cr"
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Dyrgas Gt") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428637", // "519", // Dyrgas Gate
								"2428621", // Boulder Crescent
								"2428655", // "535", // Glacier Drive South
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428655", // "535", // Glacier Drive South
								"2428644", // Canmore Shopping Area North
								"2428637", // "519" // Dyrgas Gate
						})) //
				.compileBothTripSort());
		map2.put(5l + RID_ENDS_WITH_B, new RouteTripSpec(5l + RID_ENDS_WITH_B, // 5B
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Glacier Dr", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Dyrgas Dr") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428637", // "519" // Dyrgas Gate
								"2428621", // Boulder Crescent
								"2428655", // Glacier Drive South
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428655", // Glacier Drive South
								"2428644", // Canmore Shopping Area North
								"2428637", // "519" // Dyrgas Gate
						})) //
				.compileBothTripSort());
		map2.put(6L, new RouteTripSpec(6L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Minnewanka Lk", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Banff") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428685", // "18", // Banff High School
								"2435116", // ++ Johnson Lake Road Out
								"2435120", // "607", // Lake Minnewanka
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2435120", // "607", // Lake Minnewanka
								"2435128", // ++ Two Jack Lakeside Banff Bound
								"2428685", // "18", // Banff High School
						})) //
				.compileBothTripSort());
		map2.put(10_981L, new RouteTripSpec(10_981L, // On-it
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Calgary", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Banff") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2428685", // Banff High School
								"2428657", // Canmore Benchlands Overpass South
								"2452884", // Crowfoot LRT
								"2452883", // Inter-City Express Stop #CALGARY
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2452883", // Inter-City Express Stop #CALGARY
								"2452884", // Crowfoot LRT
								"2428657", // Canmore Benchlands Overpass South
								"2428691", // Banff Train Station
								"2428685", // Banff High School
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		System.out.printf("\n%s: Unexpected compare early route!\n", routeId);
		System.exit(-1);
		return -1;
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		System.out.printf("\n%s: Unexpected split trip route!\n", mRoute.getId());
		System.exit(-1);
		return null;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		System.out.printf("\n%s: Unexpected split trip stop route!\n", mRoute.getId());
		System.exit(-1);
		return null;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		System.out.printf("\n%s: Unexpected trip %s!\n", mRoute.getId(), gTrip);
		System.exit(-1);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = AND.matcher(tripHeadsign).replaceAll(AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		System.out.printf("\nUnexptected trips to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = AND.matcher(gStopName).replaceAll(AND_REPLACEMENT);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
