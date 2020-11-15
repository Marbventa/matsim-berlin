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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

/**
* @author gmarburger
*/

/* Erzeugt Tabllen zur Analyse der Erzeugten Fahrzeuge und gibt die vollständige Verspätung wieder! */
public class PTVehicleCirculationAnalysis {
	
	public static void main (String[] args) {
		String dir = "scenarios/berlin-v5.5-10pct/caseStudy"; // opt | caseStudy
		String outputDir = dir + "/analysis";
		String prefixMATSimDirs = "/output-berlin-drt-v5.5-10pct_";
		String suffixMATSimDir = "BC_RLTVC"; // BC = Base Case, RVC = RunVehicleCirculation, RLTVC = RunLinTimVehicleCirculation bzw. BC_RLTVC, BC_RVC
		String matsimDir = dir + prefixMATSimDirs + suffixMATSimDir;
		
		String configFileName = "/berlin-drt-v5.5-10pct.output_config.xml";
		String experiencedPlansFileName = "berlin-drt-v5.5-10pct.output_experienced_plans.xml.gz";
		String networkFileName = "berlin-drt-v5.5-10pct.output_network.xml.gz";
		String transitScheduleFileName = "berlin-drt-v5.5-10pct.output_transitSchedule.xml.gz";
		String transitVehiclesFileName = "berlin-drt-v5.5-10pct.output_transitVehicles.xml.gz";
		
		Config config = ConfigUtils.loadConfig(dir + prefixMATSimDirs + suffixMATSimDir + configFileName);
		config.plans().setInputFile(experiencedPlansFileName);
		config.network().setInputFile(networkFileName);
		config.transit().setTransitScheduleFile(transitScheduleFileName);
		config.transit().setVehiclesFile(transitVehiclesFileName);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		double totalTravelTime = returnExperiencedTravelTimes(scenario.getPopulation());
		System.out.println("\nThe total duration agents spent in traffic: " + totalTravelTime);
		
		generateUmlaufVehicleAnalysis(scenario, suffixMATSimDir + "_vehicle_analysis", outputDir + "/");
	}
	
	private static void generateUmlaufVehicleAnalysis(Scenario scenario, String fileName, String outputDir) {
		TransitSchedule tS = scenario.getTransitSchedule();
		Map<Id<Vehicle>, Id<TransitRoute>> mapVecIDtoTransitMap = new HashMap<>();
		List<String> listVecIdtoTransitMap = new ArrayList<>();
		Map<Id<TransitLine>, Set<String>> mapTLtoListOfAllVecIdsThatServe = new HashMap<>();
		
		for(TransitLine tL : tS.getTransitLines().values()) {
			for (TransitRoute tR : tL.getRoutes().values()) {
				for(Departure dep : tR.getDepartures().values()) {
					mapVecIDtoTransitMap.put(dep.getVehicleId(), tR.getId());
					listVecIdtoTransitMap.add(dep.getVehicleId().toString() + "; " + tR.getId().toString() + "; " + tR.getTransportMode().toString());
					
					if(tR.getTransportMode().toString().contains("rail")) {
						if(mapTLtoListOfAllVecIdsThatServe.containsKey(tL.getId())) {
							mapTLtoListOfAllVecIdsThatServe.get(tL.getId()).add(dep.getVehicleId().toString());
						} else {
							Set<String> listOfAllVecIDs = new HashSet<>();
							listOfAllVecIDs.add(dep.getVehicleId().toString());
							mapTLtoListOfAllVecIdsThatServe.put(tL.getId(), listOfAllVecIDs);
						}
					}
				}
			}
		}
		
		for(Id<TransitLine> tLId : mapTLtoListOfAllVecIdsThatServe.keySet()) {
//			System.out.println(tLId.toString() + ": " + mapTLtoListOfAllVecIdsThatServe.get(tLId).size());

		}
		
		AnalysisUtils.writeListAsCSV(listVecIdtoTransitMap, fileName, outputDir);
	}
	
	private static double returnExperiencedTravelTimes(Population population) {
		double totaltravelTime = 0.0;
		
		for(Person person : population.getPersons().values()) {
			for(PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if(pE instanceof Leg) {
					Leg leg = (Leg) pE;
//					if(leg.getMode().equals("pt")) {
						double travelTimePerLeg = leg.getRoute().getTravelTime();
						totaltravelTime += travelTimePerLeg;	
//					}
					
				}
			}
		}
		return totaltravelTime;
	}
}
