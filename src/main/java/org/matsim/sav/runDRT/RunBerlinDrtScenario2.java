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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.drt.data.validator.DrtRequestValidator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.run.RunBerlinScenario;
import org.matsim.sav.DailyRewardHandlerSAVInsteadOfCar;
import org.matsim.sav.SAVPassengerTracker;
import org.matsim.sav.SAVPassengerTrackerImpl;
import org.matsim.sav.prepare.BerlinNetworkModification;
import org.matsim.sav.prepare.BerlinPlansModificationTagFormerCarUsers;
import org.matsim.sav.prepare.BerlinShpUtils;

/**
 * This class starts a simulation run with DRT.
 * 
 *  - The input DRT vehicles file specifies the number of vehicles and the vehicle capacity (a vehicle capacity of 1 means there is no ride-sharing).
 * 	- The DRT service area is set to the the Greater Berlin area (= the area including the Berliner Ring, see input shape file).
 * 	- The private car mode is still allowed in the Berlin city area.
 * 	- Initial plans are not modified.
 * 
 * @author ikaddoura
 */

public class RunBerlinDrtScenario2 {

	private static final Logger log = Logger.getLogger(RunBerlinDrtScenario2.class);

	static final String drtServiceAreaAttribute = "drtServiceArea";
	public static final String modeToReplaceCarTripsInBrandenburg = TransportMode.car;
	private final String taxiNetworkMode = TransportMode.car;

	private final String berlinShapeFile;
	private final String drtServiceAreaShapeFile;
	
	private Config config;
	private Scenario scenario;
	private Controler controler;
	private RunBerlinScenario berlin;
	
	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;

	private double dailyRewardDrtInsteadOfPrivateCar;

	public static void main(String[] args) {
		
		String configFileName ;
		String overridingConfigFileName;
		String berlinShapeFile;
		String drtServiceAreaShapeFile;
		double dailyRewardDrtInsteadOfPrivateCar;
		
		if (args.length > 0) {
			throw new RuntimeException();
			
		} else {		
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-drt2-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			berlinShapeFile = "scenarios/berlin-v5.2-10pct/input/berlin-shp/berlin.shp";
			drtServiceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/berliner-ring-area-shp/service-area.shp";
			dailyRewardDrtInsteadOfPrivateCar = 0.;
		}		
		
		new RunBerlinDrtScenario2( configFileName, overridingConfigFileName, berlinShapeFile, drtServiceAreaShapeFile, dailyRewardDrtInsteadOfPrivateCar).run() ;
	}
	
	public RunBerlinDrtScenario2( String configFileName, String overridingConfigFileName, String berlinShapeFile, String drtServiceAreaShapeFile, double dailyRewardDrtInsteadOfPrivateCar) {
		
		this.berlinShapeFile = berlinShapeFile;
		this.drtServiceAreaShapeFile = drtServiceAreaShapeFile;
		this.dailyRewardDrtInsteadOfPrivateCar = dailyRewardDrtInsteadOfPrivateCar;
		this.berlin = new RunBerlinScenario( configFileName, overridingConfigFileName );
	}

	public Controler prepareControler() {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
		}
		
		controler = berlin.prepareControler();
		
		// drt + dvrp module
		DrtControlerCreator.addDrtAsSingleDvrpModeToControler(controler);
		
		// reject drt requests outside the service area
		controler.addOverridingModule(new AbstractModule() {	
			@Override
			public void install() {
				this.bind(DrtRequestValidator.class).toInstance(new DrtServiceAreaRequestValidator());
			}
		});
		
		// drt fares
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});
		
		// rewards for no longer owning a car
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.addEventHandlerBinding().toInstance(new DailyRewardHandlerSAVInsteadOfCar(dailyRewardDrtInsteadOfPrivateCar, modeToReplaceCarTripsInBrandenburg));
				
				SAVPassengerTrackerImpl tracker = new SAVPassengerTrackerImpl(TransportMode.drt);		
				this.bind(SAVPassengerTracker.class).toInstance(tracker);
				this.addEventHandlerBinding().toInstance(tracker);
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
		
		BerlinShpUtils shpUtils = new BerlinShpUtils(berlinShapeFile, drtServiceAreaShapeFile);
		
		new BerlinNetworkModification(
				shpUtils,
				this.taxiNetworkMode,
				modeToReplaceCarTripsInBrandenburg,
				drtServiceAreaAttribute).run(this.scenario);
		
		new BerlinPlansModificationTagFormerCarUsers().run(scenario);
			
		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		hasPreparedScenario = true ;
		return scenario;
	}
	
	public Config prepareConfig(ConfigGroup... modulesToAdd) {
		OutputDirectoryLogging.catchLogEntries();
		
		// dvrp, drt and taxiFare config groups
		List<ConfigGroup> drtModules = new ArrayList<>();
		drtModules.add(new DvrpConfigGroup());
		drtModules.add(new DrtConfigGroup());
		drtModules.add(new TaxiFareConfigGroup());
		
		List<ConfigGroup> modules = new ArrayList<>();		
		for (ConfigGroup module : drtModules) {
			modules.add(module);
		}	
		for (ConfigGroup module : modulesToAdd) {
			modules.add(module);
		}
		
		ConfigGroup[] modulesArray = new ConfigGroup[modules.size()];
		config = berlin.prepareConfig(modules.toArray(modulesArray));		
		
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

