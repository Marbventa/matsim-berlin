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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/**
* @author gmarburger
*/

public class MatsimImportFromLinTim {
	/** Imports Data from LinTim's vehicle Scheduling.
	 * 
	 * @param scenario MATSim scenario
	 * @param isMetropolitianArea if this is set to false, it will export every TransitRoute at the same time. If true routes will be seperated for the export
	 * @param predefinedFilter At the current time only  the String "mode" is defined. May change in future.
	 * @param limitToGrossRaumBerlin if set to true, TransitRoutes inside of Berlin will be exported. If set to false only lines outside of Berlin will be exported.
	 * @param filterObject String which contains information about the Route types. These may be found under RunLinTimVehicleCirculation.createListModes()
	 * @param vehicleWorkingCounter
	 * @param pTLinkCounter
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	static Scenario run(Scenario scenario, boolean isMetropolitianArea, String predefinedFilter, boolean limitToGrossRaumBerlin, 
			String filterObject, VehicleWorkingIntegerWrapper vehicleWorkingCounter, PublicTransportLinkIntegerWrapper pTLinkCounter, String matsimExportConversionDir) 
			throws NumberFormatException, IOException {
		
		System.out.println("\n Importing.. !");
		Map<Id<Vehicle>, Integer> conversionVecIdToLines = ImportUtils.readConversionTableVehicle("vehicleToLintimLine.csv2", matsimExportConversionDir);
		
		Map<Integer, Id<Vehicle>> outputLineToMATSimVehicle = ImportUtils.readLinTimVSOutput("Vehicle_Schedules.vs", vehicleWorkingCounter, limitToGrossRaumBerlin, 
				filterObject, matsimExportConversionDir);
		
		Map<Integer, Id<Vehicle>> conversionLinTimLineToVecId = ImportUtils.readConversionTableLinTimLineToVecId("vehicleToLintimLine.csv2", matsimExportConversionDir);
		Map<Integer, List<Integer>> umlaufVecIdsWithRoutes = ImportUtils.readLinTimVSVehicleRoutesOutput("Vehicle_Schedules.vs", matsimExportConversionDir);
		
		Scenario scenarioManipulated = returnVehicleCirculationLinTimConfig(scenario, conversionVecIdToLines, outputLineToMATSimVehicle, umlaufVecIdsWithRoutes, 
				conversionLinTimLineToVecId, isMetropolitianArea, predefinedFilter, limitToGrossRaumBerlin, filterObject, pTLinkCounter);
		
		System.out.println("\n LinTim was imported regarding the mode: "  + filterObject + "!");
		return scenarioManipulated;
	}
	
	/** Will change the TransitSchedule, TransitVehicles and Network datasets in accordance with the imported conversion tables. 
	 * 
	 * @param scenarioManipulated MATSim scenario which should be edited
	 * @param conversionVecIdToLines 
	 * @param outputConversionLineToMATSimVehicle 
	 * @param umlaufVecIdsWithRoutes list of all departures which need to be served by a new vehicle-working vehicle.
	 * @param isMetropolitianArea if this is set to false, it will export every TransitRoute at the same time. If true routes will be seperated for the export
	 * @param predefinedFilter At the current time only  the String "mode" is defined. May change in future.
	 * @param limitToGrossRaumBerlin if set to true, TransitRoutes inside of Berlin will be exported. If set to false only lines outside of Berlin will be exported.
	 * @param filterObject String which contains information about the Route types. These may be found under RunLinTimVehicleCirculation.createListModes()
	 * @param ptLinkCounter Wrapper which contains an increaseable integer for PT-Link Ids.
	 * @return
	 */
	private static Scenario returnVehicleCirculationLinTimConfig(Scenario scenarioManipulated, Map<Id<Vehicle>, Integer> conversionVecIdToLines, 
			Map<Integer, Id<Vehicle>> outputConversionLineToMATSimVehicle, Map<Integer, List<Integer>> umlaufVecIdsWithRoutes, 
			Map<Integer, Id<Vehicle>> conversionLinTimLineToVecId, boolean isMetrolitianArea, String predefinedFilter, boolean limitToGrossRaumBerlin, String filterObject, 
			PublicTransportLinkIntegerWrapper ptLinkCounter) {
		Network network = scenarioManipulated.getNetwork();
		TransitSchedule transitSchedule = scenarioManipulated.getTransitSchedule();
		Vehicles transitVehicles = scenarioManipulated.getTransitVehicles();
		
		for(TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if(SelectRoutesForLinTim.filterByModeAndId(transitRoute, network, isMetrolitianArea, predefinedFilter,limitToGrossRaumBerlin,  filterObject));
				for(Departure departure : transitRoute.getDepartures().values()) {
					Id<Vehicle> departureVecId = departure.getVehicleId();
					
					if(conversionVecIdToLines.containsKey(departureVecId)) {
						int correspondingLintimLine = conversionVecIdToLines.get(departureVecId);
						Id<Vehicle> newVehicleInCirculation = outputConversionLineToMATSimVehicle.get(correspondingLintimLine);
						
						if(!transitVehicles.getVehicles().containsKey(newVehicleInCirculation)) {
							Vehicle vehicleInCirculation = VehicleUtils.getFactory().createVehicle(newVehicleInCirculation, transitVehicles.getVehicles().get(departureVecId).getType());
							transitVehicles.addVehicle(vehicleInCirculation);
						}
						System.out.println("Setting " + newVehicleInCirculation + " in " + transitRoute.getId());
						departure.setVehicleId(newVehicleInCirculation);
					}
				}
			}
		}
		
		scenarioManipulated = addConnectingLinksToNetwork(scenarioManipulated, umlaufVecIdsWithRoutes, conversionLinTimLineToVecId, ptLinkCounter);
		
		return scenarioManipulated;
	}

	/** Creates Links which connect the previously created LinTim vehicle workings.
	 * 
	 * @param scenario MATSim Scenario
	 * @param umlaufVecIdsWithRoutes list of all departures which need to be served by a new vehicle-working vehicle.
	 * @param conversionLineToMATSimVehicle map of LinTimLineIds whcih map to MATSim Vehicle Ids.
	 * @param ptLinkCounter Wrapper which contains an increaseable integer for PT-Link Ids.
	 * @return
	 */
	private static Scenario addConnectingLinksToNetwork(Scenario scenario, Map<Integer, List<Integer>> umlaufVecIdsWithRoutes, Map<Integer, Id<Vehicle>> conversionLineToMATSimVehicle,
			PublicTransportLinkIntegerWrapper ptLinkCounter) {
		Set<List<Node>> connectionsBetweenNodesMade = new HashSet<>();
		
		Network network = scenario.getNetwork();
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
		
		for(List<Integer> umlaufVehicle : umlaufVecIdsWithRoutes.values()) {
			for(int iterator = 0; iterator < umlaufVehicle.size(); iterator++) {
				// -1 indicates that the vehicle is either traveling from one end-stop to another or is simply waiting.
				if(umlaufVehicle.get(iterator) == -1) {
					int replacedVehicleBefore = umlaufVehicle.get(iterator - 1);
					// The last trip of a route should never be that it travels "empty"
					int replacedVehicleAfter = umlaufVehicle.get(iterator + 1);
					
					Id<Vehicle> idReplacedVehicleBefore = conversionLineToMATSimVehicle.get(replacedVehicleBefore);
					Id<Vehicle> idReplacedVehicleAfter = conversionLineToMATSimVehicle.get(replacedVehicleAfter);

					Id<TransitRoute> idTransitRouteBefore = TransitUtils.returnTransitRoute(idReplacedVehicleBefore);
					Id<TransitLine> idTransitLineBefore = TransitUtils.returnTransitLine(idReplacedVehicleBefore);
					Id<TransitRoute> idTransitRouteAfter = TransitUtils.returnTransitRoute(idReplacedVehicleAfter);
					Id<TransitLine> idTransitLineAfter = TransitUtils.returnTransitLine(idReplacedVehicleAfter);
					
					TransitRoute transitRouteBefore = transitSchedule.getTransitLines().get(idTransitLineBefore).getRoutes().get(idTransitRouteBefore);
					TransitRoute transitRouteAfter = transitSchedule.getTransitLines().get(idTransitLineAfter).getRoutes().get(idTransitRouteAfter);
					
					TransitRouteStop endStopOfPreviousRoute = TransitUtils.getEndStation(transitRouteBefore);
					TransitRouteStop startTopOfFollowingRoute = transitRouteAfter.getStops().get(0);
					
					Node trsEndStopNode = network.getLinks().get(endStopOfPreviousRoute.getStopFacility().getLinkId()).getToNode();
					Node trsStartStopNode = network.getLinks().get(startTopOfFollowingRoute.getStopFacility().getLinkId()).getToNode();
					
					List<Node> nodesOfLink = new ArrayList<>();
					nodesOfLink.add(trsEndStopNode);
					nodesOfLink.add(trsStartStopNode);
					
					if(!connectionsBetweenNodesMade.contains(nodesOfLink)) {
						String id = "pt_umlaufLink_" + ptLinkCounter.getPTLinkCounter();
						Id<Link> connectingLinkId = Id.createLinkId(id);
						
						Link connectingLink = NetworkUtils.createLink(connectingLinkId, trsEndStopNode, trsStartStopNode, network, 50.0, 10.0, 10000.0, 10.0);

						network.addLink(connectingLink);
						ptLinkCounter.increasePTLinkCounter();
						connectionsBetweenNodesMade.add(nodesOfLink);
					}
				}
			}
		}
		
		System.out.println("The following amount of links were added: " + connectionsBetweenNodesMade.size());
		return scenario;
	}
}
