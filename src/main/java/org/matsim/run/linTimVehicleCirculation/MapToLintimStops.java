package org.matsim.run.linTimVehicleCirculation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class MapToLintimStops {
	/** 
	 * 
	 * @param transitSchedule MATSim TransitSchedule
	 * @param network MATSim Network
	 * @param conversionMapOfStopIds 
	 * @param isMetropolitianArea if this is set to false, it will export every TransitRoute at the same time. If true routes will be seperated for the export
	 * @param predefinedFilter At the current time only  the String "mode" is defined. May change in future.
	 * @param limitToGrossRaumBerlin if set to true, TransitRoutes inside of Berlin will be exported. If set to false only lines outside of Berlin will be exported.
	 * @param filterObject String which contains information about the Route types. These may be found under RunLinTimVehicleCirculation.createListModes()
	 * @return
	 */
	static Map<Integer, LinTimStop> mapTrsToLintimStop(TransitSchedule transitSchedule, Network network, Map<Coord, Integer> conversionMapOfStopIds,
			boolean isMetropolitianArea, String predefinedFilter, boolean limitToGrossRaumBerlin, String filterObject){
		Map<Integer, LinTimStop> mapOfUniqueStops = new TreeMap<>();
		for(TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(!(SelectRoutesForLinTim.filterByModeAndId(transitRoute, network, isMetropolitianArea,  predefinedFilter,limitToGrossRaumBerlin, filterObject))) continue;
				for(TransitRouteStop trs : transitRoute.getStops()) {
					Coord trsCoord = trs.getStopFacility().getCoord();
					int lintimStopId = conversionMapOfStopIds.get(trsCoord);
					
					String shortName = trs.getStopFacility().getName();
					String longName = shortName;
					int xCoord = (int) trsCoord.getX();
					int yCoord = (int) trsCoord.getY();
					
					LinTimStop lintimStop= new LinTimStop(lintimStopId, shortName, longName, xCoord, yCoord);
					
					mapOfUniqueStops.put(lintimStopId, lintimStop);
				}
			}
		}
		
		/* create a Node and links for LinTim to this node from every other node. It helps with dijkstra.*/
		LinTimStop ultimateNode = LinTimStop.ultimateNode();
		
		mapOfUniqueStops.put(Integer.MAX_VALUE, ultimateNode);
		return mapOfUniqueStops;
	}
	
	/** Creates a Map of Coordinates from MATSim TransitRouteStops with their counterparts as Integers from LinTim Stops.
	 * 
	 * @param transitSchedule MATSim TransitSchedule
	 * @param network MATSim Network
	 * @param isMetropolitianArea if this is set to false, it will export every TransitRoute at the same time. If true routes will be seperated for the export.
	 * @param predefinedFilter At the current time only  the String "mode" is defined. May change in future.
	 * @param limitToGrossRaumBerlin if set to true, TransitRoutes inside of Berlin will be exported. If set to false only lines outside of Berlin will be exported.
	 * @param filterObject String which contains information about the Route types. These may be found under RunLinTimVehicleCirculation.createListModes()
	 * @return a mapping of Coordinates of MATSim TransitRoutesStops to an Integer which represents the Stop in LinTim.
	 * @throws IOException
	 */
	static Map<Coord, Integer> getMapOfCoordAndLintimStopId (TransitSchedule transitSchedule, Network network, 
			boolean isMetropolitianArea, String predefinedFilter, boolean limitToGrossRaumBerlin, String filterObject, String matsimConversionExportDir) throws IOException{
		Map<Coord, Integer> mapOfCoordAndLintimStopId = new HashMap<>();
		int counterLintimStopId = 1;
		
		for(TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(!(SelectRoutesForLinTim.filterByModeAndId(transitRoute, network, isMetropolitianArea, predefinedFilter, limitToGrossRaumBerlin, filterObject))) continue;
				for(TransitRouteStop transitStop : transitRoute.getStops()) {
					Coord transitStopCoord = transitStop.getStopFacility().getCoord();
					if(!mapOfCoordAndLintimStopId.keySet().contains(transitStopCoord)) {
						mapOfCoordAndLintimStopId.put(transitStopCoord, counterLintimStopId);
						counterLintimStopId +=1;
					}
				}
			}
		}		
		writeStopsToCSV2(mapOfCoordAndLintimStopId, matsimConversionExportDir);
		return mapOfCoordAndLintimStopId;
	}
	
	/** creates a file which maps Coordinates to integers which represent LinTimStopIds
	 * 
	 * @param stopConversionMap map of TransitRouteStopCoordinates with LinTim Stop Ids as integers
	 * @throws IOException
	 */
	private static void writeStopsToCSV2 (Map<Coord, Integer> stopConversionMap, String matsimConversionExportDir) throws IOException {
		String file = matsimConversionExportDir + "conversionMatSimStops.csv2";
		try (
	    		FileWriter fw = new FileWriter(file);
	        	BufferedWriter bw = new BufferedWriter(fw);
	    		) {
			bw.write("# LinTimStopId; MatsimStopX; MatsimStopY");
			bw.newLine();
			
			Set<Map.Entry<Coord, Integer>> linesEntries = stopConversionMap.entrySet();
			for(Map.Entry<Coord, Integer> linesEntry : linesEntries) {
				bw.write(linesEntry.getValue() + "; " + linesEntry.getKey().getX() + "; " + linesEntry.getKey().getY());
				bw.newLine();
			}
			bw.flush();
		}
		
		System.out.println("Conversion Table for LinTim Stops to Matsim Stops was sucessfully written!" + "\n");
	}
}
