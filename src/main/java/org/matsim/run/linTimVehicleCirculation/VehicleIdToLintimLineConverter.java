package org.matsim.run.linTimVehicleCirculation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;


public class VehicleIdToLintimLineConverter {
	/** creates a conversion table, which maps a LinTimLineId to a MATSim TransitVehicleId. Every LinTimLineId is thus unique.
	 * 
	 * @param transitSchedule MATSim TransitSchedule
	 * @param network MATSim Network
	 * @param isMetropolitianArea if this is set to false, it will export every TransitRoute at the same time. If true routes will be seperated for the export
	 * @param predefinedFilter At the current time only  the String "mode" is defined. May change in future.
	 * @param limitToGrossRaumBerlin if set to true, TransitRoutes inside of Berlin will be exported. If set to false only lines outside of Berlin will be exported.
	 * @param filterObject String which contains information about the Route types. These may be found under RunLinTimVehicleCirculation.createListModes()
	 * @return a Map with key MATSim Vehicle Ids and gives them a LinTimLineId as integer.
	 * @throws IOException if filtering is not possible or contains an error.
	 */
	static Map<Id<Vehicle>, Integer> createAndPrintConversionTableLintimLineId(TransitSchedule transitSchedule, Network network,
			boolean isMetropolitianArea, String predefinedFilter, boolean limitToGrossRaumBerlin, String filterObject, String matsimConversionExportDir) throws IOException{
		Map<Id<Vehicle>, Integer> conversionMapOfVecIdToLintimLineId = new TreeMap<>();
		int counterLineId = 1;
		
		for(TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(!(SelectRoutesForLinTim.filterByModeAndId(transitRoute, network, isMetropolitianArea, predefinedFilter, limitToGrossRaumBerlin, filterObject))) continue;
				for(Departure departure : transitRoute.getDepartures().values()) {
					Id<Vehicle> departureVecId = departure.getVehicleId();
					conversionMapOfVecIdToLintimLineId.put(departureVecId, counterLineId);
					counterLineId +=1;
				}
			}
		}
	
		
	writeMapToCSV2(conversionMapOfVecIdToLintimLineId, matsimConversionExportDir);	
	return conversionMapOfVecIdToLintimLineId;
	}
	
	/** Writes a CVS2 table for MATSim exports with input of Vehicles. Left column: LinTimLineId, right column: MatsimVehicleId.
	 * 
	 * @param vehicleConversionMap a map of MATSim VehicleIds and their corresponding LinTimLineIds.
	 * @throws IOException
	 */
	private static void writeMapToCSV2 (Map<Id<Vehicle>, Integer> vehicleConversionMap, String matsimConversionExportDir) throws IOException {
		String file = matsimConversionExportDir + "vehicleToLintimLine.csv2";
		try (
	    		FileWriter fw = new FileWriter(file);
	        	BufferedWriter bw = new BufferedWriter(fw);
	    		) {
			bw.write("LintimLineId; MatsimVehicleId");
			bw.newLine();
			
			Set<Map.Entry<Id<Vehicle>, Integer>> linesEntries = vehicleConversionMap.entrySet();
			for(Map.Entry<Id<Vehicle>, Integer> linesEntry : linesEntries) {
				bw.write(linesEntry.getValue() + "; " + linesEntry.getKey());
				bw.newLine();
			}
			bw.flush();
		}
		
		System.out.println("Conversion Table for LintimLines to Matsim Vehicles was sucessfully written!" + "\n");
	}
}
