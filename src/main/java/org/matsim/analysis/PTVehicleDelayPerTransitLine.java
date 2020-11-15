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

package org.matsim.analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
* @author gmarburger
*/

public class PTVehicleDelayPerTransitLine {

	/* Berechnet verspätung für iene bestimmte Linie! */
	public static void main(String[] args) throws IOException {
		String dir = "scenarios/berlin-v5.5-10pct/CaseStudy/Delay/U9/Sims/RVC/output-berlin-drt-v5.5-10pct_BC_RVC_005";
//		String dir = "scenarios/berlin-v5.5-10pct/CaseStudy/output-berlin-drt-v5.5-10pct_BC_RLTVC"; //Präfix: BC, BC_RLTVC, BC_RVC
		String configFileName = "/berlin-drt-v5.5-10pct.output_config.xml";
		String experiencedPlansFileName = "berlin-drt-v5.5-10pct.output_experienced_plans.xml.gz";
		String networkFileName = "berlin-drt-v5.5-10pct.output_network.xml.gz";
		String transitScheduleFileName = "berlin-drt-v5.5-10pct.output_transitSchedule.xml.gz";
		String transitVehiclesFileName = "berlin-drt-v5.5-10pct.output_transitVehicles.xml.gz";
		String eventsFile = "/berlin-drt-v5.5-10pct.output_events.xml.gz";
		
		Config config = ConfigUtils.loadConfig(dir + configFileName);
		config.plans().setInputFile(experiencedPlansFileName);
		config.network().setInputFile(networkFileName);
		config.transit().setTransitScheduleFile(transitScheduleFileName);
		config.transit().setVehiclesFile(transitVehiclesFileName);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);	
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		ArrivesTooLateHandler handler = new ArrivesTooLateHandler(); 
		eventsManager.addHandler(handler);
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readFile(dir + eventsFile);
		List<VehicleDepartsAtFacilityEvent> eventsHandled = handler.returnList();
		
		String tLIdString;
		Set <Id<TransitStopFacility>> facilityOfTransitLine = new HashSet<>();
		Set <Id<Vehicle>> vehiclesOfTransitLine = new HashSet<>();
		for(TransitLine tL : scenario.getTransitSchedule().getTransitLines().values()) {
//			if(tL.getId().toString().contains("U1---17512_4") || tL.getId().toString().contains("U2---17514_4") || tL.getId().toString().contains("U3---17515_4") ||
//				tL.getId().toString().contains("U6---17521_4") || tL.getId().toString().contains("U7---17523_4") || tL.getId().toString().contains("U8---17525_4")) {
//			if(tL.getId().toString().contains("M44---17454_7")) {
//			if(tL.getId().toString().contains("104---17291_7") || tL.getId().toString().contains("112---17297_7") || tL.getId().toString().contains("12---17279_7") || 
//				tL.getId().toString().contains("140---17316_7") || tL.getId().toString().contains("156---17322_7") || tL.getId().toString().contains("162---17327_7") || 
//				tL.getId().toString().contains("170---17333_7") || tL.getId().toString().contains("171---17334_7") || tL.getId().toString().contains("179---17337_7") || 
//				tL.getId().toString().contains("184---17340_7") || tL.getId().toString().contains("188---17343_7") || tL.getId().toString().contains("204---17351_7") || 
//				tL.getId().toString().contains("240---17360_7") || tL.getId().toString().contains("246---17362_7") || tL.getId().toString().contains("248---17364_7") || 
//				tL.getId().toString().contains("255---17367_7") || tL.getId().toString().contains("260---17370_7") || tL.getId().toString().contains("271---17375_7") || 
//				tL.getId().toString().contains("277---17377_7") || tL.getId().toString().contains("284---17380_7") || tL.getId().toString().contains("370---17405_7") || 
//				tL.getId().toString().contains("255---17367_7") || tL.getId().toString().contains("260---17370_7") || tL.getId().toString().contains("271---17375_7") || 
//				tL.getId().toString().contains("377---17409_7") || tL.getId().toString().contains("629---18929_3") || tL.getId().toString().contains("711---9696_7") || 
//				tL.getId().toString().contains("742---15714_7") || tL.getId().toString().contains("M21---17447_7") || tL.getId().toString().contains("M27---17448_7") || 
//				tL.getId().toString().contains("M77---17462_7") || tL.getId().toString().contains("M46---17456_7") || tL.getId().toString().contains("M76---17461_7") || 
//				tL.getId().toString().contains("N10---17474_7") || tL.getId().toString().contains("N12---18942_7") || tL.getId().toString().contains("N60---17494_7") || 
//				tL.getId().toString().contains("N79---17502_7") || tL.getId().toString().contains("N8---17472_7") || tL.getId().toString().contains("N81---17503_7") || 
//				tL.getId().toString().contains("N84---17504_7") || tL.getId().toString().contains("N88---17505_7") || tL.getId().toString().contains("N9---17473_7") || 
//				tL.getId().toString().contains("Sud---17013_7") || tL.getId().toString().contains("U7---17522_7") || tL.getId().toString().contains("U9---19369_7") || 
//				tL.getId().toString().contains("X11---17530_7") || tL.getId().toString().contains("X76---17537_7") ){
				
			if(tL.getId().toString().contains("U9---17526_4")) {
//				System.out.println(tL.getId());
				tLIdString = tL.getName();
				for(TransitRoute tR : tL.getRoutes().values()) {
					for(TransitRouteStop stop : tR.getStops()) {
							facilityOfTransitLine.add(stop.getStopFacility().getId());
					}
					for(Departure dep : tR.getDepartures().values()) {
						vehiclesOfTransitLine.add(dep.getVehicleId());
					}
				}
			}
		}
//		System.out.println("\n" + facilityOfTransitLine.size());
		
		double tLDelay = 0.0;
		for(VehicleDepartsAtFacilityEvent event : eventsHandled) {
			Id<TransitStopFacility> tsFacilityId  = event.getFacilityId();
			double delayAtFacility = event.getDelay();
			if(facilityOfTransitLine.contains(tsFacilityId) && vehiclesOfTransitLine.contains(event.getVehicleId())) {
				if(scenario.getActivityFacilities().getFacilities().get(event.getFacilityId()).getLinkId().toString().contains("pt_43433") && event.getTime() > 7.4 * 3600)
					System.out.println(delayAtFacility);
				tLDelay = tLDelay + delayAtFacility;
//				System.out.println(event.getVehicleId());
			}
		}
		System.out.println("\n" + tLDelay);
	}
}