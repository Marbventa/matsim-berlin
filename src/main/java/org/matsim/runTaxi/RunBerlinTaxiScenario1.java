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

package org.matsim.runTaxi;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.data.validator.TaxiRequestValidator;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.prepare.BerlinNetworkModification;
import org.matsim.prepare.BerlinPlansModificationSplitTrips;
import org.matsim.prepare.BerlinShpUtils;
import org.matsim.run.RunBerlinScenario;

/**
 * This class starts a simulation run with taxis.
 * 
 *  - The input taxi vehicles file specifies the number of vehicles and the vehicle capacity (a vehicle capacity of 1 means there is no ride-sharing).
 * 	- The taxi service area is set to the the Greater Berlin area (= the area including the Berliner Ring, see input shape file).
 * 	- The private car mode is no longer allowed in the Berlin city area (see input shape file) and may only be used for trips within Brandenburg (network mode: 'car_bb').
 * 	- Initial plans are modified in the following way:
 * 		- Car trips within the Berlin area are replaced by DRT trips.
 * 		- Car trips from Brandenburg to Berlin or the other way round are replaced by 4 alternatives: a direct pt trip and 3 park-and-ride trips (car_bb + S / RB / DRT) 
 * 
 * @author ikaddoura
 */

public class RunBerlinTaxiScenario1 {

	private static final Logger log = Logger.getLogger(RunBerlinTaxiScenario1.class);

	static final String taxiServiceAreaAttribute = "taxiServiceArea";

	private final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction");
	private final String inputPersonAttributesSubpopulationPerson = "person";

	public static final String modeToReplaceCarTripsInBrandenburg = "car_bb"; // needs to match the mode specifications in the config file
	private final String modeToReplaceCarTripsInBerlin = TransportMode.taxi;
	private final String modeToReplaceCarTripsToFromBerlin = TransportMode.pt;
	private final String taxiNetworkMode = TransportMode.car; // needs to match the mode specification in the config file
	
	private final boolean splitTripsS = true; 
	private final boolean splitTripsRB = true; 
	private final boolean splitTripsTaxi = true; 
	private final String parkAndRideActivity = "park-and-ride";
	private final double parkAndRideDuration = 60.;
	
	private final String transitStopCoordinatesSFile;
	private final String transitStopCoordinatesRBFile;
	private final String berlinShapeFile;
	private final String serviceAreaShapeFile;
	
	private Config config;
	private Scenario scenario;
	private Controler controler;
	private RunBerlinScenario berlin;
	
	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;

	public static void main(String[] args) {
		
		String configFileName ;
		String overridingConfigFileName;
		String berlinShapeFile;
		String drtServiceAreaShapeFile;
		String transitStopCoordinatesSFile;
		String transitStopCoordinatesRBFile;
		
		if (args.length > 0) {
			throw new RuntimeException();
			
		} else {		
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-taxi1-v5.2-1pct.config.xml"; // berlin 1pct
			overridingConfigFileName = null;
			berlinShapeFile = "scenarios/berlin-v5.2-10pct/input/berlin-shp/berlin.shp";
			drtServiceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/berliner-ring-area-shp/service-area.shp";
			transitStopCoordinatesSFile = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_S-zoneC.csv";
			transitStopCoordinatesRBFile = "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_RB-zoneC.csv";
		}		
		
		new RunBerlinTaxiScenario1( configFileName, overridingConfigFileName, berlinShapeFile, drtServiceAreaShapeFile, transitStopCoordinatesSFile, transitStopCoordinatesRBFile).run() ;
	}
	
	public RunBerlinTaxiScenario1( String configFileName, String overridingConfigFileName, String berlinShapeFile, String drtServiceAreaShapeFile, String transitStopCoordinatesSFile, String transitStopCoordinatesRBFile) {
		
		this.transitStopCoordinatesSFile = transitStopCoordinatesSFile;
		this.transitStopCoordinatesRBFile = transitStopCoordinatesRBFile;
		this.berlinShapeFile = berlinShapeFile;
		this.serviceAreaShapeFile = drtServiceAreaShapeFile;
				
		this.berlin = new RunBerlinScenario( configFileName, overridingConfigFileName );
	}

	public Controler prepareControler() {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
		}
		
		controler = berlin.prepareControler();
		
		// taxi + dvrp module
		TaxiControlerCreator.addTaxiAsSingleDvrpModeToControler(controler);
		
		// reject taxi requests outside the service area
		controler.addOverridingModule(new AbstractModule() {	
			@Override
			public void install() {
				this.bind(TaxiRequestValidator.class).toInstance(new TaxiServiceAreaRequestValidator());
			}
		});
		
		// taxi fares
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});
		
		hasPreparedControler = true ;
		return controler;
	}
	
	public Scenario prepareScenario() {
		if ( !hasPreparedConfig ) {
			prepareConfig( ) ;
		}
		
		scenario = berlin.prepareScenario();
		
		BerlinShpUtils shpUtils = new BerlinShpUtils(berlinShapeFile, serviceAreaShapeFile);
		
		new BerlinNetworkModification(shpUtils,
				this.taxiNetworkMode,
				modeToReplaceCarTripsInBrandenburg,
				taxiServiceAreaAttribute).run(this.scenario);
		
		new BerlinPlansModificationSplitTrips(transitStopCoordinatesSFile,
				transitStopCoordinatesRBFile,
				shpUtils,
				inputPersonAttributesSubpopulationPerson,
				modeToReplaceCarTripsInBerlin,
				modeToReplaceCarTripsInBrandenburg,
				modeToReplaceCarTripsToFromBerlin,
				stageActivities,
				parkAndRideActivity,
				parkAndRideDuration,
				splitTripsS,
				splitTripsRB,
				splitTripsTaxi).run(scenario);	
			
		hasPreparedScenario = true ;
		return scenario;
	}
	
	public Config prepareConfig(ConfigGroup... modulesToAdd) {
		OutputDirectoryLogging.catchLogEntries();
		
		// dvrp, taxi and taxiFare config groups
		List<ConfigGroup> taxiModules = new ArrayList<>();
		taxiModules.add(new DvrpConfigGroup());
		taxiModules.add(new TaxiConfigGroup());
		taxiModules.add(new TaxiFareConfigGroup());
		
		List<ConfigGroup> modules = new ArrayList<>();		
		for (ConfigGroup module : taxiModules) {
			modules.add(module);
		}	
		for (ConfigGroup module : modulesToAdd) {
			modules.add(module);
		}
		
		ConfigGroup[] modulesArray = new ConfigGroup[modules.size()];
		config = berlin.prepareConfig(modules.toArray(modulesArray));		
				
		TaxiControlerCreator.adjustTaxiConfig(config);
		
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

