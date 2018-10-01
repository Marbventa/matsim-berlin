/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.sav.runDRT;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.sav.prepare.BerlinNetworkModification;
import org.matsim.sav.prepare.BerlinPlansModificationSplitTrips;
import org.matsim.sav.prepare.BerlinShpUtils;

/**
 * This class starts a simulation run with DRT.
 * 
 *  - The input DRT vehicles file specifies the number of vehicles and the vehicle capacity (a vehicle capacity of 1 means there is no ride-sharing).
 * 	- The DRT service area is set to the the inner-city Berlin area (see input shape file).
 * 	- The private car mode is no longer allowed in the inner-city area (see input shape file) and may only be used for trips outside the inner-city area (network mode: 'car_bb').
 * 	- Initial plans are modified in the following way:
 * 		- Car trips within the Berlin area are replaced by DRT trips.
 * 		- Car trips from Brandenburg to Berlin or the other way round are replaced by 4 alternatives: a direct pt trip and 3 park-and-ride trips (car_bb + S / RB / DRT) 
 * 
 * @author ikaddoura
 */

public class RunBerlinDrtScenario1 {

	private static final Logger log = Logger.getLogger(RunBerlinDrtScenario1.class);

	static final String drtServiceAreaAttribute = "drtServiceArea";

	private final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction");
	private final String inputPersonAttributesSubpopulationPerson = "person";

	public static final String modeToReplaceCarTripsInBrandenburg = "car_bb"; // needs to match the mode specifications in the config file
	private final String modeToReplaceCarTripsInBerlin = TransportMode.drt;
	private final String modeToReplaceCarTripsToFromBerlin = TransportMode.pt;
	private final String taxiNetworkMode = TransportMode.car; // needs to match the mode specification in the config file
	
	private final boolean splitTripsS = true; 
	private final boolean splitTripsTaxi = true; 
	private final String parkAndRideActivity = "park-and-ride";
	private final double parkAndRideDuration = 60.;
	
	private final String transitStopCoordinatesSFile;
	private final String carRestrictedAreaShapeFile;
	private final String drtServiceAreaShapeFile;
	
	private Config config;
	private Scenario scenario;
	private Controler controler;
	private RunBerlinDrtScenario berlin;
	
	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;

	public static void main(String[] args) {
		
		String configFileName ;
		String overridingConfigFileName;
		String berlinShapeFile;
		String drtServiceAreaShapeFile;
		String transitStopCoordinatesSFile;
		
		if (args.length > 0) {
			throw new RuntimeException();
			
		} else {		
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-drt1-v5.2-1pct.config.xml"; // berlin 1pct
			overridingConfigFileName = null;
			berlinShapeFile = "scenarios/berlin-v5.2-10pct/input/berlin-shp/berlin.shp";
			drtServiceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/berliner-ring-area-shp/service-area.shp";
			transitStopCoordinatesSFile = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_S-zoneC.csv";
		}		
		
		new RunBerlinDrtScenario1( configFileName, overridingConfigFileName, berlinShapeFile, drtServiceAreaShapeFile, transitStopCoordinatesSFile).run() ;
	}
	
	public RunBerlinDrtScenario1( String configFileName, String overridingConfigFileName, String carRestrictedAreaShapeFile, String drtServiceAreaShapeFile, String transitStopCoordinatesSFile) {
		this.transitStopCoordinatesSFile = transitStopCoordinatesSFile;
		this.carRestrictedAreaShapeFile = carRestrictedAreaShapeFile;
		this.drtServiceAreaShapeFile = drtServiceAreaShapeFile;	
		this.berlin = new RunBerlinDrtScenario( configFileName, overridingConfigFileName );
	}

	public Controler prepareControler() {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
		}
		
		controler = berlin.prepareControler();
		
		hasPreparedControler = true ;
		return controler;
	}
	
	public Scenario prepareScenario() {
		if ( !hasPreparedConfig ) {
			prepareConfig( ) ;
		}
		
		scenario = berlin.prepareScenario();
		
		BerlinShpUtils shpUtils = new BerlinShpUtils(carRestrictedAreaShapeFile, drtServiceAreaShapeFile);
		new BerlinNetworkModification(shpUtils).addSAVandReplaceCarMode(scenario, taxiNetworkMode, modeToReplaceCarTripsInBrandenburg, drtServiceAreaAttribute);	
		new BerlinPlansModificationSplitTrips(transitStopCoordinatesSFile,
				shpUtils,
				inputPersonAttributesSubpopulationPerson,
				modeToReplaceCarTripsInBerlin,
				modeToReplaceCarTripsInBrandenburg,
				modeToReplaceCarTripsToFromBerlin,
				stageActivities,
				parkAndRideActivity,
				parkAndRideDuration,
				splitTripsS,
				splitTripsTaxi).run(scenario);	

		hasPreparedScenario = true ;
		return scenario;
	}
	
	public Config prepareConfig(ConfigGroup... modulesToAdd) {
		
		config = berlin.prepareConfig(modulesToAdd);			
		DrtControlerCreator.adjustDrtConfig(config);
		
		hasPreparedConfig = true ;
		return config ;
	}
	
	 public void run() {
		if ( !hasPreparedControler ) {
			prepareControler() ;
		}
		controler.run();
		log.info("Done.");
	}

}
