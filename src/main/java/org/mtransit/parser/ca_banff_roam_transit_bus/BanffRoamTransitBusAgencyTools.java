package org.mtransit.parser.ca_banff_roam_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
		this.serviceIds = extractUsefulServiceIds(args, this, true);
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
	private static final String S = "s";
	private static final String X = "x";

	private static final long RID_ENDS_WITH_B = 2_000L;
	private static final long RID_ENDS_WITH_S = 19_000L;
	private static final long RID_ENDS_WITH_X = 24_000L;

	@Override
	public long getRouteId(GRoute gRoute) {
		String rsn = gRoute.getRouteShortName();
		if (Utils.isDigitsOnly(rsn)) {
			return Long.parseLong(rsn);
		}
		if ("On-it".equals(rsn)) {
			return 10_981L;
		}
		Matcher matcher = DIGITS.matcher(rsn);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			String rsnLC = rsn.toLowerCase(Locale.ENGLISH);
			if (rsnLC.endsWith(B)) {
				return RID_ENDS_WITH_B + digits;
			} else if (rsnLC.endsWith(S)) {
				return RID_ENDS_WITH_S + digits;
			} else if (rsnLC.endsWith(X)) {
				return RID_ENDS_WITH_X + digits;
			}
		}
		System.out.printf("\nUnexpected route ID for %s!\n", gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final Pattern STARTS_WITH_ROUTE_RID = Pattern.compile("(route [0-9]{1} (\\- )?)", Pattern.CASE_INSENSITIVE);

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = STARTS_WITH_ROUTE_RID.matcher(routeLongName).replaceAll(StringUtils.EMPTY);
		routeLongName = CleanUtils.CLEAN_AND.matcher(routeLongName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
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
		map2.put(3L, new RouteTripSpec(3L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Banff HS", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Canmore") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428659", // "", // Canmore 9th Street
								"2428657", // ++ Canmore Benchlands Overpass South
								"2428656", // ++ Canmore Holiday Inn
								"2428700", // != Rotary Park
								"2428680", // != Banff Train Station Elk Street
								"2428685", // Banff High School
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428685", // Banff High School
								"2428661", // ++ Canmore Collegiate
								"2428659", // Canmore 9th Street
						})) //
				.compileBothTripSort());
		map2.put(8L + RID_ENDS_WITH_S, new RouteTripSpec(8L + RID_ENDS_WITH_S, // 8S
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Lk Louise", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Banff") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"2428685", // Banff High School Transit Hub
									"2512558", // ++
									"2483623", // Lake Louise Lakeshore
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"2483623", // Lake Louise Lakeshore
								"2512557", // ++
								"2428685", // Banff High School Transit Hub
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (mRoute.getId() == 1L) {
			if (gTrip.getDirectionId() == null) {
				if ("Banff Gondola".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.SOUTH.intValue());
					return;
				}
				if ("Downtown Banff".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.NORTH.intValue());
					return;
				}
			}
		}
		if (mRoute.getId() == 2L) {
			if (gTrip.getDirectionId() == null) {
				if ("Banff".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.SOUTH.intValue());
					return;
				}
				if ("Tunnel Mountain".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.NORTH.intValue());
					return;
				}
			}
		}
		if (mRoute.getId() == 4L) {
			if (gTrip.getDirectionId() == null) {
				if ("Cave and Basin".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.SOUTH.intValue());
					return;
				}
				if ("Banff".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.NORTH.intValue());
					return;
				}
			}
			if (gTrip.getDirectionId() == 1) {
				if ("Cave and Basin".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.SOUTH.intValue());
					return;
				}
			}
		}
		if (mRoute.getId() == 5L) {
			if (gTrip.getDirectionId() == null) {
				if ("Three Sisters".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.SOUTH.intValue());
					return;
				}
				if ("Cougar Creek".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.NORTH.intValue());
					return;
				}
			}
		}
		if (mRoute.getId() == 6L) {
			if (gTrip.getDirectionId() == null) {
				if ("Banff".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.SOUTH.intValue());
					return;
				}
				if ("Lake Minnewanka".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.NORTH.intValue());
					return;
				}
			}
		}
		if (mRoute.getId() == 7L) {
			if (gTrip.getDirectionId() == null) {
				if ("Banff Centre".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.SOUTH.intValue());
					return;
				}
				if ("Downtown Banff".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.NORTH.intValue());
					return;
				}
			}
		}
		if (mRoute.getId() == 8L) {
			if (gTrip.getDirectionId() == null) {
				if ("Banff".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.SOUTH.intValue());
					return;
				}
				if ("Lake Louise".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.NORTH.intValue());
					return;
				}
			}
		}
		if (mRoute.getId() == 8L + RID_ENDS_WITH_X) { // 8X
			if (gTrip.getDirectionId() == null) {
				if ("Banff".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.SOUTH.intValue());
					return;
				}
				if ("Lake Louise".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.NORTH.intValue());
					return;
				}
			}
		}
		if (mRoute.getId() == 9L) {
			if (gTrip.getDirectionId() == null) {
				if ("Banff".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.SOUTH.intValue());
					return;
				}
				if ("Johnston Canyon".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.NORTH.intValue());
					return;
				}
			}
		}
		if (mRoute.getId() == 10L) {
			if (gTrip.getDirectionId() == null) {
				if ("Banff".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.SOUTH.intValue());
					return;
				}
				if ("Moraine Lake".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.NORTH.intValue());
					return;
				}
			}
		}
		if (mRoute.getId() == 10_981L) { // On-it
			if (gTrip.getDirectionId() == null) {
				if ("Banff".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.WEST.intValue());
					return;
				}
				if ("Calgary".equals(gTrip.getTripHeadsign())) {
					mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), MDirectionType.EAST.intValue());
					return;
				}
			}
		}
		System.out.printf("\n%s: Unexpected trip %s!\n", mRoute.getId(), gTrip);
		System.exit(-1);
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
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
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
