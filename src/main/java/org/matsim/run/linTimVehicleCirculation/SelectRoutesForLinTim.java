/********************************************************************** *
  project: org.matsim.
                                                                          
  ********************************************************************** *
                                                                          
  copyright       : (C) 2020 by the members listed in the COPYING,        
                    LICENSE and WARRANTY file.                            
  email           : info at matsim dot org                                
                                                                          
  ********************************************************************** *
                                                                          
    This program is free software; you can redistribute it and/or modify  
    it under the terms of the GNU General Public License as published by  
    the Free Software Foundation; either version 2 of the License, or     
    (at your option) any later version.                                   
    See also COPYING, LICENSE and WARRANTY file                           
                                                                          
  ********************************************************************** */

package org.matsim.run.linTimVehicleCirculation;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

/**
* @author gmarburger
*/

class SelectRoutesForLinTim {
	/** Filters a TransitRoute if it should be exported or not. This divides the different transport modes in separate export Files.
	 *  For further information please check the technical documentation
	 * 
	 * @param transitRoute TransitRoute to check
	 * @param network MATSim Network
	 * @param isMetropolitianArea if this is set to false, it will export every TransitRoute at the same time. If true the code will continue.
	 * @param predefinedFilter If set to "mode" it will continue. Other Types of filter are not yet defined.
	 * @param limitToGrossRaumBerlin if set to true, Busses will be split into 'Berlin' or 'not berlin'. If set to false, it will export routes outside of Berlin.
	 * @param filterObject String which contains information about the Route types. These may be found under RunLinTimVehicleCirculation.createListModes()
	 * @return
	 */
	static boolean filterByModeAndId(TransitRoute transitRoute, Network network, boolean isMetropolitianArea, String predefinedFilter, boolean limitToGrossRaumBerlin, 
			 String filterObject) {
		boolean filter = false;
		if(!isMetropolitianArea) return true;
		
		if (predefinedFilter == "mode") {
			String mode = transitRoute.getTransportMode();
			if(mode.toString().contains(filterObject) && filterObject.contains("tram")) return true;
			
			if(mode.toString().contains("rail") && filterObject.contains("rail")) {
				return idIndicatesWhichRailToFilter(transitRoute, filterObject);
			}
			
			// 
			if(mode == filterObject && filterObject.contains("bus")) {
				if(limitToGrossRaumBerlin) {
					// returns false, if the first TransitRouteStop is outside of Berlin (30km farther then Brandenburger Tor).
					for(TransitRouteStop trs : transitRoute.getStops()) {
						if(!(booleanLessThen30kmFromBBTor(trs.getStopFacility().getCoord()))) return false;
					}
					return true;
				} else {
					// returns true, if the first TRS is outside of Berlin.
					for(TransitRouteStop trs : transitRoute.getStops()) {
						if(!(booleanLessThen30kmFromBBTor(trs.getStopFacility().getCoord()))) return true;
					}
					return false;
				}
			}
		} else {
			System.out.println("Filter is not defined! Please check for Predefinded Filter in main!");
			throw new NullPointerException();
		}
		return filter;
	}
	
	/** Filters a rail TransitRoute for each of the different Rail modes which are incompatible. Check RunLinTimVehicleCirculation.createListModes() for Modes
	 * 
	 * @param transitRoute TransitRoute should be of mode rail. 
	 * @param filterObject String of createListModes()
	 * @return true when the TransitRoute can be categorized by createListModes() otherwise false.
	 */
	private static boolean idIndicatesWhichRailToFilter(TransitRoute transitRoute, String filterObject) {
		String tRIdString = transitRoute.getId().toString();
		if(filterObject.contains("railU") && tRIdString.contains("U")) return true;
		if(filterObject.contains("railS") && tRIdString.contains("S"))  return true;
		if(filterObject.contains("railRE") && tRIdString.contains("RE")) return true;
		if(filterObject.contains("railZ") && !(tRIdString.contains("U") || tRIdString.contains("S") || tRIdString.contains("RE"))) return true;
		if(filterObject.contains("rail-")) return true;
		else return false;
	}
	
	/* returns true, if the stops aren't in at least 30km of Berlin. Currently only checking for first stop! :( */
	/** Checks if coordinates lie within a distance of 30km.
	 * 
	 * @param coordiantesOfTRS coordinates of a TransitRoute Stop
	 * @return returns true, if coordinates are within 30km of Berlin Brandenburger Tor.
	 */
	private static boolean booleanLessThen30kmFromBBTor(Coord coordiantesOfTRS){
		double brandenBurgerTorX = 4593914.263441454;
		double brandenburgerTorY = 5821274.294229268;
		
		double trsCoordX = coordiantesOfTRS.getX();
		double trsCoordY = coordiantesOfTRS.getY();
		
		double maxDifInCoordinates = nineHundredQkmOfCoordsAsDouble();
		
		double difX = brandenBurgerTorX - trsCoordX;
		double difY = brandenburgerTorY - trsCoordY;
		double difInPythagoras = Math.pow(difX, 2) + Math.pow(difY ,2);
		
		if(difInPythagoras <= maxDifInCoordinates) {
			return true;
		} else {
			return false;
		}
	}
	
	/** Returns a double which is the equivalent to the distance of 30km.
	 * 
	 * @return Double which equals 30km in coordinate distances
	 */
	private static double nineHundredQkmOfCoordsAsDouble() {
		double brandenBurgerTorX = 4593914.263441454;
		double brandenburgerTorY = 5821274.294229268;
		
		// geltowKaserne is ungefähr 30km entfernt
		double geltowKaserneX = 4566228.16674397;
		double geltowKaserneY = 5806328.160231036;
		
		double difX = brandenBurgerTorX - geltowKaserneX;
		double difY = brandenburgerTorY - geltowKaserneY;
		
		return Math.pow(difX, 2) + Math.pow(difY, 2);
	}
}
