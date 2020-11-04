package org.matsim.run.linTimVehicleCirculation;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.vehicles.Vehicle;

public class TransitUtils {
	/** Returns the last stop of a Transit Route.
	 * 
	 * @param transitRoute A MATSim TransitRoute
	 * @return TransitRouteStop of the TransitRoute.
	 */
	public static TransitRouteStop getEndStation (TransitRoute transitRoute) {
		int numberOfStops = transitRoute.getStops().size();
		TransitRouteStop endStation = transitRoute.getStops().get(numberOfStops -1);
		return endStation;
	}
	
	/* This only works for standart Berlin Matsim TransitVehicles! */
	public static Id<TransitRoute> returnTransitRoute(Id<Vehicle> transitVehicleId) {
		String tVIdString = transitVehicleId.toString();
		// pt_1---1879_900_0_0
		int indexOfPtUnderscore = tVIdString.indexOf("_");
		String removePt = tVIdString.substring(indexOfPtUnderscore+1);
		
		int indexOfLastUnderscoreVehicleNr = removePt.lastIndexOf("_");
		String removeVecNumber = removePt.substring(0, indexOfLastUnderscoreVehicleNr);
		
		Id<TransitRoute> idTransitRoute = Id.create(removeVecNumber, TransitRoute.class);
		return idTransitRoute;
	}
	
	/* Also only works for Berlin Scenario hence not in use! */ 
	public static Id<TransitLine> returnTransitLine(Id<Vehicle> transitVehicleId){
		String transitRouteIdString = returnTransitRoute(transitVehicleId).toString();
		
		int indexOfLastUnderscoreTransitRouteNr = transitRouteIdString.lastIndexOf("_");
		String removeTransitRouteNr = transitRouteIdString.substring(0, indexOfLastUnderscoreTransitRouteNr);
		
		Id<TransitLine> transitLineId = Id.create(removeTransitRouteNr, TransitLine.class);
		return transitLineId;
	}
}
