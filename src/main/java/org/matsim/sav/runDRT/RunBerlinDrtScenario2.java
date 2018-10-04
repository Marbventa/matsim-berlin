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
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.sav.DailyRewardHandlerSAVInsteadOfCar;
import org.matsim.sav.SAVPassengerTracker;
import org.matsim.sav.SAVPassengerTrackerImpl;
import org.matsim.sav.prepare.BerlinNetworkModification;
import org.matsim.sav.prepare.BerlinPlansModificationTagFormerCarUsers;
import org.matsim.sav.prepare.BerlinShpUtils;
import org.matsim.sav.prepare.PersonAttributesModification;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * This class starts a simulation run with DRT.
 * 
 *  - The input DRT vehicles file specifies the number of vehicles and the vehicle capacity (a vehicle capacity of 1 means there is no ride-sharing).
 * 	- The DRT service area is set to the the inner-city Berlin area (see input shape file).
 * 	- The private car mode is still allowed in the Berlin city area.
 * 	- Initial plans are not modified.
 * 
 * @author ikaddoura
 */

public class RunBerlinDrtScenario2 {

	private static final Logger log = Logger.getLogger(RunBerlinDrtScenario2.class);
	private final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction");

	static final String drtServiceAreaAttribute = "drtServiceArea";
	public static final String modeToReplaceCarTripsInBrandenburg = TransportMode.car;
	private final String taxiNetworkMode = TransportMode.car;

	private final String drtServiceAreaShapeFile;
	
	private Config config;
	private Scenario scenario;
	private Controler controler;
	private RunBerlinDrtScenario berlin;
	
	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;

	private double dailyRewardDrtInsteadOfPrivateCar;

	public static void main(String[] args) {
		
		String configFileName ;
		String overridingConfigFileName;
		String drtServiceAreaShapeFile;
		double dailyRewardDrtInsteadOfPrivateCar;
		
		if (args.length > 0) {
			throw new RuntimeException();
			
		} else {		
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-drt2-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			drtServiceAreaShapeFile = "scenarios/berlin-v5.2-10pct/input/shp-inner-city-area/inner-city-area.shp";
			dailyRewardDrtInsteadOfPrivateCar = 0.;
		}		
		
		new RunBerlinDrtScenario2( configFileName, overridingConfigFileName, drtServiceAreaShapeFile, dailyRewardDrtInsteadOfPrivateCar).run() ;
	}
	
	public RunBerlinDrtScenario2( String configFileName, String overridingConfigFileName, String drtServiceAreaShapeFile, double dailyRewardDrtInsteadOfPrivateCar) {
		this.drtServiceAreaShapeFile = drtServiceAreaShapeFile;
		this.dailyRewardDrtInsteadOfPrivateCar = dailyRewardDrtInsteadOfPrivateCar;
		this.berlin = new RunBerlinDrtScenario( configFileName, overridingConfigFileName );
	}

	public Controler prepareControler() {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
		}
		
		controler = berlin.prepareControler();
		
		if (dailyRewardDrtInsteadOfPrivateCar != 0.) {
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
		}
		
		// different modes for different subpopulations
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				
				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				
				List<String> availableModesArrayList = new ArrayList<>();
				availableModesArrayList.add("bicycle");
				availableModesArrayList.add("pt");
				availableModesArrayList.add("walk");
				availableModesArrayList.add(modeToReplaceCarTripsInBrandenburg);
				
				final String[] availableModes = availableModesArrayList.toArray(new String[availableModesArrayList.size()]);
				
				addPlanStrategyBinding("SubtourModeChoice_no-potential-sav-user").toProvider(new Provider<PlanStrategy>() {
										
					@Inject
					Scenario sc;

					@Override
					public PlanStrategy get() {
						
						log.info("SubtourModeChoice_no-potential-sav-user" + " - available modes: " + availableModes.toString());
						final String[] chainBasedModes = {modeToReplaceCarTripsInBrandenburg, "bicycle"};

						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new SubtourModeChoice(sc.getConfig()
								.global()
								.getNumberOfThreads(), availableModes, chainBasedModes, false, 
								0.5, tripRouterProvider));
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
						return builder.build();
					}
				});			
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
		
		BerlinShpUtils shpUtils = new BerlinShpUtils(drtServiceAreaShapeFile);
		new BerlinNetworkModification(shpUtils).addSAVmode(scenario, taxiNetworkMode, drtServiceAreaAttribute);
		new BerlinPlansModificationTagFormerCarUsers().run(scenario);
		new PersonAttributesModification(shpUtils, stageActivities).run(scenario);

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

