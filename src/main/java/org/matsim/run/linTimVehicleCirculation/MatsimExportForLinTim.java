package org.matsim.run.linTimVehicleCirculation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

public class MatsimExportForLinTim {
	/** Exports a specific transport mode from MATSim to LinTim.
	 * 
	 * @param scenario MATSim Scenario
	 * @param isMetropolitianArea if this is set to false, it will export every TransitRoute at the same time. If true routes will be seperated for the export
	 * @param predefinedFilter At the current time only  the String "mode" is defined. May change in future.
	 * @param limitToGrossRaumBerlin if set to true, TransitRoutes inside of Berlin will be exported. If set to false only lines outside of Berlin will be exported.
	 * @param filterObject String which contains information about the Route types. These may be found under RunLinTimVehicleCirculation.createListModes()
	 * @throws IOException
	 */
	static void export(Scenario scenario,  boolean isMetropolitianArea, String predefinedFilter, boolean limitToGrossRaumBerlin,
			String filterObject, String matsimConversionExportDir) throws IOException {
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
		Network network = scenario.getNetwork();
		
		System.out.println("\n" + "******* Commencing LinTim Export " + filterObject +" ! *******" + "\n");
		
		Map<Id<Vehicle>, Integer> conversionMapOfLineIds = VehicleIdToLintimLineConverter.createAndPrintConversionTableLintimLineId(transitSchedule, network, 
				isMetropolitianArea, predefinedFilter, limitToGrossRaumBerlin, filterObject, matsimConversionExportDir);
		
		Map<Coord, Integer> conversionMapOfStops = MapToLintimStops.getMapOfCoordAndLintimStopId(transitSchedule, network, 
				isMetropolitianArea, predefinedFilter, limitToGrossRaumBerlin,  filterObject, matsimConversionExportDir);
				
		System.out.println("Input for LinTim being written...");
		createStopFile(transitSchedule, network, conversionMapOfStops, isMetropolitianArea, predefinedFilter,limitToGrossRaumBerlin,  filterObject);
		
		createEdgeFile(transitSchedule, network, conversionMapOfStops, isMetropolitianArea, predefinedFilter,limitToGrossRaumBerlin,  filterObject);
				
		createTripsFile(transitSchedule, network, conversionMapOfLineIds, conversionMapOfStops, isMetropolitianArea, predefinedFilter,limitToGrossRaumBerlin,  filterObject);
		System.out.println("\n" + "******* Export " + filterObject +" concluded! *******");
	}
	
	/** Creates the necessary Trips file for LinTim.
	 * 
	 * @param transitSchedule MATSim TransitSchedule
	 * @param network MATSim Network
	 * @param conversionMapVecIdToLinTimLineId Map of MATSim Vehicle Ids with their LinTimLineIds as Integers.
	 * @param conversionMapOfStops MATSim TransitRouteStops Coordinates with matching LinTim Stop Ids as integers
	 * @param isMetropolitianArea if this is set to false, it will export every TransitRoute at the same time. If true routes will be seperated for the export
	 * @param predefinedFilter At the current time only  the String "mode" is defined. May change in future.
	 * @param limitToGrossRaumBerlin if set to true, TransitRoutes inside of Berlin will be exported. If set to false only lines outside of Berlin will be exported.
	 * @param filterObject String which contains information about the Route types. These may be found under RunLinTimVehicleCirculation.createListModes()
	 */
	static void createTripsFile(TransitSchedule transitSchedule,  Network network, Map<Id<Vehicle>, Integer> conversionMapVecIdToLinTimLineId, 
			 Map<Coord, Integer> conversionMapOfStops, boolean isMetropolitianArea, String predefinedFilter,boolean limitToGrossRaumBerlin, String filterObject) {
		List<String> tripList = new ArrayList<>();
		
		for(TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			// Predefined or irrelevant factors are set to -1
			int startId = -1;
			int periodicStartId = -1;
			int startStation;
			int startTime;				
			int endId = -1;
			int periodicEndId = -1;
			int endStation;
			int endTime;
			int line;
				
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(!(SelectRoutesForLinTim.filterByModeAndId(transitRoute, network, isMetropolitianArea, predefinedFilter, limitToGrossRaumBerlin, filterObject))) continue;
				
				TransitRouteStop startStop = transitRoute.getStops().get(0);
				startStation = conversionMapOfStops.get(startStop.getStopFacility().getCoord());
					
				TransitRouteStop endStop = TransitUtils.getEndStation(transitRoute);
				endStation = conversionMapOfStops.get(endStop.getStopFacility().getCoord());
					
				for(Departure departure : transitRoute.getDepartures().values()) {
					Id<Vehicle> vecId = departure.getVehicleId();
					line = conversionMapVecIdToLinTimLineId.get(vecId);
						
					startTime = (int) departure.getDepartureTime();
					endTime = (int) (departure.getDepartureTime() + endStop.getDepartureOffset());
						
					LinTimTripEntry trip = new LinTimTripEntry(startId, periodicStartId, startStation, startTime, endId, periodicEndId, endStation, endTime, line);
					tripList.add(trip.toString());
				}
			}
		}
		
		ExportToLintimUtils.writeListToLinTimCSV2("Trips", tripList);
	}
	
	/** Creates the necessary Stops file for LinTim.
	 * 
	 * @param transitSchedule MATSim TransitSchedule
	 * @param network MATSim Network
	 * @param conversionMapOfStopIds
	 * @param isMetropolitianArea if this is set to false, it will export every TransitRoute at the same time. If true routes will be seperated for the export.
	 * @param predefinedFilter At the current time only  the String "mode" is defined. May change in future.
	 * @param limitToGrossRaumBerlin if set to true, TransitRoutes inside of Berlin will be exported. If set to false only lines outside of Berlin will be exported.
	 * @param filterObject String which contains information about the Route types. These may be found under RunLinTimVehicleCirculation.createListModes()
	 */
	static void createStopFile(TransitSchedule transitSchedule, Network network, Map<Coord, Integer> conversionMapOfStopIds, boolean isBerlin, String predefinedFilter, 
			boolean limitToGrossRaumBerlin, String filterObject) {
		Map<Integer, LinTimStop> mapOfLinTimStopsById = MapToLintimStops.mapTrsToLintimStop(transitSchedule, network, conversionMapOfStopIds, 
				isBerlin, predefinedFilter, limitToGrossRaumBerlin, filterObject);
		Set<String> printableSetOfStops = new TreeSet<>();
		
		for(LinTimStop lintimStop : mapOfLinTimStopsById.values()) {
			String combinedAttributesStop = ExportToLintimUtils.combineStringsWithSemicolon(String.valueOf(lintimStop.stop_id),lintimStop.short_name, lintimStop.short_name,
					String.valueOf(lintimStop.x_coordinate), String.valueOf(lintimStop.y_coordinate));
			printableSetOfStops.add(combinedAttributesStop);
		}
		
		ExportToLintimUtils.writeListToLinTimCSV2("Stop", printableSetOfStops);
	}
	
	/** Creates the nessesary Edge file for LinTim.
	 * 
	 * @param transitSchedule MATSim Transit Schedule
	 * @param network MATSim network.
	 * @param conversionMapOfStops MATSim TransitRouteStops Coordinates with matching LinTim Stop Ids as integers
	 * @param isMetropolitianArea if this is set to false, it will export every TransitRoute at the same time. If true routes will be seperated for the export
	 * @param predefinedFilter At the current time only  the String "mode" is defined. May change in future.
	 * @param limitToGrossRaumBerlin if set to true, TransitRoutes inside of Berlin will be exported. If set to false only lines outside of Berlin will be exported.
	 * @param filterObject String which contains information about the Route types. These may be found under RunLinTimVehicleCirculation.createListModes()
	 */
	static void createEdgeFile(TransitSchedule transitSchedule, Network network, Map<Coord, Integer> conversionMapOfStops, boolean isMetropolitianArea, 
			String predefinedFilter, boolean limitToGrossRaumBerlin, String filterObject){
		Map<List<Integer>, LinTimEdge> mapLinkToEdge= new HashMap<>();
		int counterEdgeId = 1;
		
		for(TransitLine transitLine : transitSchedule.getTransitLines().values()) {			
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(!(SelectRoutesForLinTim.filterByModeAndId(transitRoute, network, isMetropolitianArea, predefinedFilter,limitToGrossRaumBerlin,  filterObject))) continue;
				
				TreeMap<Integer, TransitRouteStop> mapOfTransitRouteStopsOrdered = getTreeMapOfTransitRouteStops(transitRoute);
				
				Set<Map.Entry<Integer, TransitRouteStop>> routeStopsOrderedEntries = mapOfTransitRouteStopsOrdered.entrySet();
				for(Map.Entry<Integer, TransitRouteStop> routeStopsOrderedEntry :routeStopsOrderedEntries) {
					if(routeStopsOrderedEntry.getKey() == 1) continue;
					int key = routeStopsOrderedEntry.getKey();
					TransitRouteStop tRS = routeStopsOrderedEntry.getValue();
					int offSetToStop = (int) tRS.getArrivalOffset();
					int offSetFromStop = (int) mapOfTransitRouteStopsOrdered.get(key -1).getDepartureOffset();
					
					int bound = offSetToStop - offSetFromStop;
					
					Id<Link> linkIdOfTRS = tRS.getStopFacility().getLinkId();
					Link linkOfTRS = network.getLinks().get(linkIdOfTRS);
					int length = (int) linkOfTRS.getLength();
					
					int linTimEdgeId =  counterEdgeId;
					int fromStop = conversionMapOfStops.get(mapOfTransitRouteStopsOrdered.get(key -1).getStopFacility().getCoord());
					int toStop = conversionMapOfStops.get(tRS.getStopFacility().getCoord());
					int headway = 5;
					
					List<Integer> listFromToNode = new ArrayList<>();
					listFromToNode.add(fromStop);
					listFromToNode.add(toStop);
					
					// This step saves about 5k on links.
					LinTimEdge edgeFromTRS = new LinTimEdge(linTimEdgeId, fromStop, toStop, length, bound, bound, headway);
					if(!mapLinkToEdge.containsKey(listFromToNode)) {
						mapLinkToEdge.put(listFromToNode, edgeFromTRS);
						counterEdgeId +=1;
					} else {
						int durationNewConnection = edgeFromTRS.getUpperBound();
						int durationOldConnection = mapLinkToEdge.get(listFromToNode).getUpperBound();
						if(durationNewConnection > durationOldConnection) {
							mapLinkToEdge.put(listFromToNode, edgeFromTRS);
							counterEdgeId +=1;
						}
					}
				}
			}
		}
		
		
		Map<List<Integer>, LinTimEdge> appendableUlimateLinks = createUltimateLinks(conversionMapOfStops, counterEdgeId);
		
		List<String> printableSetOfEdges = new ArrayList<>();
//		List<String> printableSetOfHeadways = new ArrayList<>();
		mapLinkToEdge.putAll(appendableUlimateLinks);
		Map<Integer, LinTimEdge> sortedEdgeMap = new TreeMap<>();
//		for(LinTimEdge lintimEdge : linTimEdgeSet) {
		for(LinTimEdge lintimEdge : mapLinkToEdge.values()) {
			sortedEdgeMap.put(lintimEdge.link_index, lintimEdge);
		}
		
		Set<Map.Entry<Integer, LinTimEdge>> sortedEdgeEntries = sortedEdgeMap.entrySet();
		for(Map.Entry<Integer, LinTimEdge> sortedEdgeEntry: sortedEdgeEntries) {
			printableSetOfEdges.add(sortedEdgeEntry.getValue().toString());
			
//			String combinedAttributesHeadway = ExportToLintimUtils.combineStringsWithSemicolon(String.valueOf(lintimEdge.link_index), String.valueOf(lintimEdge.headway));
//			printableSetOfHeadways.add(combinedAttributesHeadway);
		}
		
		ExportToLintimUtils.writeListToLinTimCSV2("Edge", printableSetOfEdges);
//		ExportToLintimUtils.writeListToLinTimCSV2("Headway", printableSetOfHeadways);
	}
	
	/** 
	 * 
	 * @param conversionMapOfStops 
	 * @param MinLinTimEdgeIdNr
	 * @return
	 */
	private static Map<List<Integer>, LinTimEdge> createUltimateLinks(Map<Coord, Integer> conversionMapOfStops, int MinLinTimEdgeIdNr) {
		Map<List<Integer>, LinTimEdge> appendableUltimateLinks = new HashMap<>();
		
		LinTimStop ultimateNode = LinTimStop.ultimateNode();
		
		int counterEdgeId = MinLinTimEdgeIdNr;
		int length = 1;
		int lower_bound = Integer.MAX_VALUE;
		int upper_bound = lower_bound;
		int headway = 5;
		for(Integer stopId : conversionMapOfStops.values()) {
			LinTimEdge ultimateEdge = new LinTimEdge(counterEdgeId, ultimateNode.getStopId(), stopId, length, lower_bound, upper_bound, headway);
			List<Integer> fromToNodeList = new ArrayList<>();
			fromToNodeList.add(ultimateNode.getStopId());
			fromToNodeList.add(stopId);
			appendableUltimateLinks.put(fromToNodeList, ultimateEdge);
			
			counterEdgeId +=1;
		}
		
		return appendableUltimateLinks;
	}

	static TreeMap<Integer, TransitRouteStop> getTreeMapOfTransitRouteStops(TransitRoute transitRoute){
		TreeMap<Integer, TransitRouteStop> treeMapOfTransitRouteStops = new TreeMap<>();
		int iterator = 1;
		for(TransitRouteStop transitRouteStop : transitRoute.getStops()) {
			treeMapOfTransitRouteStops.put(iterator, transitRouteStop);
			iterator +=1;
		}
		return treeMapOfTransitRouteStops;
	}


}
